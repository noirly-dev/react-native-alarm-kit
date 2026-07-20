#import "AlarmKitBootstrap.h"
#import <UserNotifications/UserNotifications.h>

@implementation AlarmKitBootstrap

+ (void)load {
  dispatch_async(dispatch_get_main_queue(), ^{
    [AlarmKitBootstrap registerNotificationDelegate];
  });
}

+ (void)registerNotificationDelegate {
  Class delegateClass = NSClassFromString(@"NoirlyAlarmKit.AlarmNotificationDelegate");
  if (delegateClass == nil) {
    return;
  }
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
  id shared = [delegateClass performSelector:NSSelectorFromString(@"shared")];
#pragma clang diagnostic pop
  if (shared != nil) {
    [UNUserNotificationCenter currentNotificationCenter].delegate = shared;
  }
}

@end
