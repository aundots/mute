package com.mute.shutter.ads

/**
 * 전체화면 광고(전면·앱 오픈)가 짧은 간격으로 연달아 뜨지 않도록 막는 공용 게이트.
 * 예: 재부팅 후 앱을 열면 앱 오픈 광고가 뜨고, 곧바로 무음 재적용 성공 전면 광고가
 * 이어지면 사용성·정책 모두 나쁘므로 최소 간격을 둔다.
 *
 * SDK에 의존하지 않는 순수 코틀린이라 free/paid 두 플레이버가 공유한다.
 */
object AdFrequencyGate {
    private const val MIN_GAP_MS = 60_000L

    @Volatile
    private var lastShownAt = 0L

    /** 마지막 전체화면 광고 이후 최소 간격이 지났는지 */
    fun canShowFullScreen(now: Long = System.currentTimeMillis()): Boolean =
        now - lastShownAt >= MIN_GAP_MS

    /** 전체화면 광고가 실제로 노출된 순간 호출 */
    fun markShown(now: Long = System.currentTimeMillis()) {
        lastShownAt = now
    }
}
