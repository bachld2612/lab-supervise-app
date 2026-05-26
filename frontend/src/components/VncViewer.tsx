import { Box, CircularProgress, Typography } from '@mui/material';
import { createExamRoomVncSession, createVncSession } from 'api/vnc';
import { useEffect, useRef, useState } from 'react';

interface VncViewerProps {
  classId: number;
  studentUserId: number;
  isOnline: boolean;
  mode?: 'class' | 'exam-room';
}

type Status = 'idle' | 'connecting' | 'connected' | 'error';

const WS_BASE = (() => {
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const host = import.meta.env.VITE_APP_API_URL
    ? new URL(import.meta.env.VITE_APP_API_URL as string).host
    : window.location.host;
  return `${proto}://${host}`;
})();

const MAX_RETRIES = 5;
const RETRY_DELAY_MS = 3000;

export default function VncViewer({ classId, studentUserId, isOnline, mode = 'class' }: VncViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const rfbRef = useRef<InstanceType<typeof import('@novnc/novnc/core/rfb').default> | null>(null);
  const [status, setStatus] = useState<Status>('idle');
  const retryCountRef = useRef(0);
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!isOnline) {
      if (retryTimerRef.current) clearTimeout(retryTimerRef.current);
      retryTimerRef.current = null;
      retryCountRef.current = 0;
      rfbRef.current?.disconnect();
      rfbRef.current = null;
      setStatus('idle');
      return;
    }

    let cancelled = false;
    retryCountRef.current = 0;

    const connect = async () => {
      if (cancelled) return;
      setStatus('connecting');

      const scheduleRetry = () => {
        if (cancelled) return;
        if (retryCountRef.current < MAX_RETRIES) {
          retryCountRef.current += 1;
          retryTimerRef.current = setTimeout(connect, RETRY_DELAY_MS);
        } else {
          setStatus('error');
        }
      };

      try {
        const res =
          mode === 'exam-room'
            ? await createExamRoomVncSession(classId, studentUserId)
            : await createVncSession(classId, studentUserId);
        if (cancelled || !containerRef.current) return;

        const { default: RFB } = await import('@novnc/novnc/core/rfb');
        if (cancelled || !containerRef.current) return;

        const rfb = new RFB(containerRef.current, `${WS_BASE}/vnc-relay?token=${res.data.token}`);
        rfb.viewOnly = true;
        rfb.scaleViewport = true;
        rfb.qualityLevel = 6;

        rfb.addEventListener('connect', () => {
          retryCountRef.current = 0;
          setStatus('connected');
        });
        rfb.addEventListener('disconnect', () => {
          if (cancelled) return;
          rfbRef.current = null;
          scheduleRetry();
        });

        rfbRef.current = rfb;
      } catch {
        scheduleRetry();
      }
    };

    connect();

    return () => {
      cancelled = true;
      if (retryTimerRef.current) clearTimeout(retryTimerRef.current);
      retryTimerRef.current = null;
      rfbRef.current?.disconnect();
      rfbRef.current = null;
    };
  }, [classId, studentUserId, isOnline, mode]);

  return (
    <Box sx={{ width: '100%', aspectRatio: '16/9', bgcolor: '#111', position: 'relative', overflow: 'hidden' }}>
      {status === 'idle' && (
        <Overlay>
          <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
            Offline
          </Typography>
        </Overlay>
      )}
      {status === 'connecting' && (
        <Overlay>
          <CircularProgress size={18} thickness={4} />
        </Overlay>
      )}
      {status === 'error' && (
        <Overlay>
          <Typography variant="caption" color="error.light">
            Không thể kết nối
          </Typography>
        </Overlay>
      )}
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
    </Box>
  );
}

function Overlay({ children }: { children: React.ReactNode }) {
  return (
    <Box
      sx={{
        position: 'absolute',
        inset: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1
      }}
    >
      {children}
    </Box>
  );
}
