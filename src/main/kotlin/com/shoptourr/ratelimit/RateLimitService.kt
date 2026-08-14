package com.shoptourr.ratelimit

import com.shoptourr.config.RateLimitProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class RateLimitService(
	private val redis: StringRedisTemplate,
	private val properties: RateLimitProperties,
	private val clock: Clock,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun consume(bucket: String, id: String, limit: Int): RateLimitDecision {
		val windowSeconds = properties.window.seconds.coerceAtLeast(1)
		val now = clock.instant().epochSecond
		val window = now / windowSeconds
		val resetEpochSeconds = (window + 1) * windowSeconds
		val key = "rl:$bucket:$id:$window"
		return try {
			val count = redis.opsForValue().increment(key) ?: 1L
			if (count == 1L) {
				redis.expire(key, properties.window)
			}
			val remaining = (limit - count).coerceAtLeast(0L).toInt()
			RateLimitDecision(
				allowed = count <= limit,
				limit = limit,
				remaining = remaining,
				resetEpochSeconds = resetEpochSeconds,
			)
		} catch (ex: RedisConnectionFailureException) {
			allowOnStoreFailure(limit, resetEpochSeconds, ex)
		} catch (ex: RedisSystemException) {
			allowOnStoreFailure(limit, resetEpochSeconds, ex)
		}
	}

	private fun allowOnStoreFailure(limit: Int, resetEpochSeconds: Long, ex: Exception): RateLimitDecision {
		log.warn("Rate-limit store unavailable; allowing request", ex)
		return RateLimitDecision(
			allowed = true,
			limit = limit,
			remaining = limit,
			resetEpochSeconds = resetEpochSeconds,
		)
	}
}

data class RateLimitDecision(
	val allowed: Boolean,
	val limit: Int,
	val remaining: Int,
	val resetEpochSeconds: Long,
)
