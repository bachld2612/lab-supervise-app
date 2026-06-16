declare module '@novnc/novnc/core/rfb' {
  interface RFBOptions {
    credentials?: { password?: string };
    wsProtocols?: string[];
  }

  export default class RFB {
    constructor(target: HTMLElement, url: string, options?: RFBOptions);
    viewOnly: boolean;
    scaleViewport: boolean;
    resizeSession: boolean;
    qualityLevel: number;
    addEventListener(type: 'connect' | 'disconnect' | 'credentialsrequired' | 'securityfailure' | 'clipboard' | 'bell' | 'desktopname', listener: (e: CustomEvent) => void): void;
    removeEventListener(type: string, listener: (e: CustomEvent) => void): void;
    disconnect(): void;
    sendCredentials(credentials: { password: string }): void;
  }
}
