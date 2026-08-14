package com.shoptourr.fx

import com.shoptourr.config.FxProperties
import com.shoptourr.trip.FxCatalog
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class FxRateService(
	private val live: LiveFxClient,
	private val redis: StringRedisTemplate,
	private val properties: FxProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun quote(tripCurrency: String, quoteCurrency: String): FxCatalog.Quote {
		val from = tripCurrency.uppercase()
		val to = quoteCurrency.uppercase()
		if (from == to) {
			return FxCatalog.quote(from, to)
		}
		liveRate(from, to)?.let { rate ->
			return FxCatalog.Quote(rate.setScale(SCALE, RoundingMode.HALF_UP), LIVE_PROVIDER)
		}
		return FxCatalog.quote(from, to)
	}

	private fun liveRate(from: String, to: String): BigDecimal? {
		if (!properties.enabled) {
			return null
		}
		val rates = cachedRates(from) ?: fetchAndCache(from) ?: return null
		return rates[to]
	}

	private fun cachedRates(base: String): Map<String, BigDecimal>? {
		return try {
			redis.opsForValue().get(cacheKey(base))?.let { decode(it) }
		} catch (ex: RedisConnectionFailureException) {
			log.warn("FX cache unavailable", ex)
			null
		} catch (ex: RedisSystemException) {
			log.warn("FX cache unavailable", ex)
			null
		}
	}

	private fun fetchAndCache(base: String): Map<String, BigDecimal>? {
		val rates = live.rates(base) ?: return null
		try {
			redis.opsForValue().set(cacheKey(base), encode(rates), properties.cacheTtl)
		} catch (ex: RedisConnectionFailureException) {
			log.warn("FX cache write failed", ex)
		} catch (ex: RedisSystemException) {
			log.warn("FX cache write failed", ex)
		}
		return rates
	}

	companion object {
		const val LIVE_PROVIDER = "live"
		private const val SCALE = 6

		fun cacheKey(base: String): String = "fx:live:${base.uppercase()}"

		internal fun encode(rates: Map<String, BigDecimal>): String =
			rates.entries.joinToString("|") { "${it.key}:${it.value.toPlainString()}" }

		internal fun decode(raw: String): Map<String, BigDecimal> =
			raw.split('|').mapNotNull { pair ->
				val sep = pair.indexOf(':')
				if (sep <= 0) {
					null
				} else {
					val code = pair.substring(0, sep)
					val amount = pair.substring(sep + 1).toBigDecimalOrNull() ?: return@mapNotNull null
					code to amount
				}
			}.toMap()
	}
}
