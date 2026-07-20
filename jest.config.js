module.exports = {
  preset: 'react-native',
  setupFiles: ['<rootDir>/jest.setup.ts'],
  modulePathIgnorePatterns: ['<rootDir>/example/', '<rootDir>/lib/'],
  testPathIgnorePatterns: ['types.test-d.ts'],
};
