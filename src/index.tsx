import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-sbp-pay' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';
interface SbpRequest {
  test: string;
}

interface sbpUrl {
  url: string;
  packageName: string;
}

const SbpPay = NativeModules.SbpPay
  ? NativeModules.SbpPay
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function multiply(a: number, b: number): Promise<number> {
  return SbpPay.multiply(a, b);
}

export function checkUrl(a: Partial<SbpRequest>): Promise<String> {
  return SbpPay.checkUrl(a);
}

export function openSbpDeepLinkInBank(a: Partial<sbpUrl>): Promise<String> {
  return SbpPay.openSbpDeepLinkInBank(a);
}
