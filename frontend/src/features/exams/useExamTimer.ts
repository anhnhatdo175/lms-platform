import { useEffect, useRef, useState } from 'react'

import { remainingMs } from './examClock'

/**
 * Đếm ngược tới `expiresAt` theo giờ server; gọi `onElapsed` đúng một lần khi về 0. `expiresAt` là `null` khi
 * chưa có phiên hoặc bài không còn IN_PROGRESS: khi đó không đếm.
 */
export function useExamTimer(
  expiresAt: string | null,
  offsetMs: number,
  onElapsed: () => void,
): number | null {
  const [now, setNow] = useState(() => Date.now())
  const onElapsedRef = useRef(onElapsed)
  const firedRef = useRef(false)

  useEffect(() => {
    onElapsedRef.current = onElapsed
  }, [onElapsed])

  useEffect(() => {
    if (expiresAt === null) return
    firedRef.current = false
    const tick = () => {
      const current = Date.now()
      setNow(current)
      if (remainingMs(expiresAt, offsetMs, current) === 0 && !firedRef.current) {
        firedRef.current = true
        onElapsedRef.current()
      }
    }
    tick()
    const timer = window.setInterval(tick, 500)
    return () => window.clearInterval(timer)
  }, [expiresAt, offsetMs])

  return expiresAt === null ? null : remainingMs(expiresAt, offsetMs, now)
}
