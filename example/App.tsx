import React, {useCallback, useEffect, useState} from 'react';
import {
  Alert,
  Button,
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  AlarmKit,
  isAlarmKitError,
  type AlarmHandle,
  type PermissionStatus,
  type PlatformCapabilities,
} from '@noirly-forge/react-native-alarm-kit';

export default function App() {
  const [alarms, setAlarms] = useState<AlarmHandle[]>([]);
  const [permissions, setPermissions] = useState<PermissionStatus | null>(null);
  const [capabilities, setCapabilities] = useState<PlatformCapabilities | null>(null);
  const [lastEvent, setLastEvent] = useState<string>('none');

  const refresh = useCallback(async () => {
    const [all, perms, caps] = await Promise.all([
      AlarmKit.getAllAlarms(),
      AlarmKit.checkPermissions(),
      AlarmKit.getCapabilities(),
    ]);
    setAlarms(all);
    setPermissions(perms);
    setCapabilities(caps);
  }, []);

  useEffect(() => {
    refresh().catch(console.error);

    const unsubscribers = [
      AlarmKit.addListener('onAlarmFired', alarm => {
        setLastEvent(`fired: ${alarm.title}`);
        refresh();
      }),
      AlarmKit.addListener('onAlarmMissedThenFired', alarm => {
        setLastEvent(`missed-then-fired: ${alarm.title}`);
        refresh();
      }),
      AlarmKit.addListener('onAlarmsReconciled', reconciled => {
        setLastEvent(`reconciled: ${reconciled.length}`);
        refresh();
      }),
      AlarmKit.addListener('onSnoozed', alarm => {
        setLastEvent(`snoozed: ${alarm.title}`);
        refresh();
      }),
      AlarmKit.addListener('onDismissed', alarm => {
        setLastEvent(`dismissed: ${alarm.title}`);
        refresh();
      }),
      AlarmKit.addListener('onPermissionsChanged', status => {
        setPermissions(status);
      }),
    ];

    return () => unsubscribers.forEach(unsub => unsub());
  }, [refresh]);

  const scheduleTestAlarm = async () => {
    try {
      await AlarmKit.requestPermissions();
      const triggerAtMillis = Date.now() + 60_000;
      await AlarmKit.scheduleAlarm({
        triggerAtMillis,
        title: 'Test alarm',
        recurrenceRule: {frequency: 'none'},
        payload: JSON.stringify({source: 'example'}),
        snoozeConfig: {defaultMinutes: 5, maxSnoozeCount: 3},
      });
      await refresh();
      Alert.alert('Scheduled', 'Alarm will fire in 60 seconds');
    } catch (error) {
      if (isAlarmKitError(error)) {
        Alert.alert('Error', error.code);
      } else {
        Alert.alert('Error', String(error));
      }
    }
  };

  const cancelAll = async () => {
    await AlarmKit.cancelAllAlarms();
    await refresh();
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>AlarmKit Example</Text>
      <Text style={styles.subtitle}>Last event: {lastEvent}</Text>

      <View style={styles.row}>
        <Button title="Request permissions" onPress={() => AlarmKit.requestPermissions().then(refresh)} />
        <Button title="Schedule +1 min" onPress={scheduleTestAlarm} />
        <Button title="Cancel all" onPress={cancelAll} />
      </View>

      {permissions && (
        <Text style={styles.meta}>
          Notifications: {permissions.notificationStatus} | Exact: {permissions.exactAlarmStatus}
        </Text>
      )}
      {capabilities && (
        <Text style={styles.meta}>
          Exact alarms: {String(capabilities.supportsExactAlarms)} | Cap:{' '}
          {capabilities.maxPendingAlarms ?? 'none'}
        </Text>
      )}

      <FlatList
        data={alarms}
        keyExtractor={item => item.id}
        renderItem={({item}) => (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>{item.title}</Text>
            <Text>{new Date(item.triggerAtMillis).toLocaleString()}</Text>
            <Text>State: {item.state}</Text>
            <View style={styles.row}>
              <Button title="Snooze" onPress={() => AlarmKit.snoozeAlarm(item.id).then(refresh)} />
              <Button title="Dismiss" onPress={() => AlarmKit.dismissAlarm(item.id).then(refresh)} />
              <Button title="Cancel" onPress={() => AlarmKit.cancelAlarm(item.id).then(refresh)} />
            </View>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>No alarms scheduled</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 12},
  title: {fontSize: 24, fontWeight: '700'},
  subtitle: {color: '#555'},
  meta: {fontSize: 12, color: '#666'},
  row: {flexDirection: 'row', flexWrap: 'wrap', gap: 8},
  card: {padding: 12, borderWidth: 1, borderColor: '#ddd', borderRadius: 8, marginTop: 8},
  cardTitle: {fontWeight: '600'},
  empty: {marginTop: 24, textAlign: 'center', color: '#888'},
});
