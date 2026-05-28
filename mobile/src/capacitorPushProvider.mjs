import { PushNotifications } from '@capacitor/push-notifications';

export function createCapacitorPushProvider() {
  return {
    requestPermissions: () => PushNotifications.requestPermissions(),
    register: () => new Promise((resolve, reject) => {
      const registrationListener = PushNotifications.addListener('registration', (token) => {
        cleanup();
        resolve(token);
      });
      const errorListener = PushNotifications.addListener('registrationError', (error) => {
        cleanup();
        reject(error);
      });

      const cleanup = () => {
        void Promise.resolve(registrationListener).then((listener) => listener.remove());
        void Promise.resolve(errorListener).then((listener) => listener.remove());
      };

      void PushNotifications.register();
    }),
  };
}
