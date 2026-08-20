package com.shoptourr.shared.validation

/**
 * Shared regexes for request bodies. Jakarta `@Pattern` and Valix `@Pattern` both match the
 * entire string, so these are the same shapes the mobile client enforces in `FieldRules`.
 */
object FieldPatterns {
	const val PERSON_OR_PLACE = """^(?=.*\p{L})[\p{L}\p{M} .,'’\-]+$"""
	const val ITEM_OR_PLACE = """^(?=.*[\p{L}\p{N}])[\p{L}\p{M}\p{N} .,'’\-()/&+]+$"""
	const val ISO_4217 = """^[A-Z]{3}$"""
	const val ISO_3166_1_ALPHA_2 = """^[A-Z]{2}$"""
	const val LOCALE = """^(en|ru)$"""
	const val HEX_COLOR = """^#[0-9A-Fa-f]{6}$"""
	const val SHA256_HEX = """^[0-9a-fA-F]{64}$"""
	const val IMAGE_CONTENT_TYPE = """^image/(jpeg|jpg|png|webp|heic|heif)$"""
	const val DEVICE_NAME = """^(?=.*[\p{L}\p{N}])[\p{L}\p{M}\p{N} ._-]+$"""
	const val APP_VERSION = """^[0-9A-Za-z._+\-]{1,64}$"""
	const val AVATAR_GLYPH = """^[\p{L}\p{M}]{1,2}$"""
	const val MOOD = """^(?=.*[\p{L}\p{So}])[\p{L}\p{M}\p{So} ]+$"""
	const val REQUIRED_TEXT = """^(?=.*[\p{L}\p{N}])[\P{Cc}\n\r\t]*$"""
	const val OPTIONAL_TEXT = """^[\P{Cc}\n\r\t]*$"""
	const val PUSH_TOKEN = """^[A-Za-z0-9:_.=\-]+$"""
	const val OIDC_NONCE = """^[A-Za-z0-9_-]+$"""
}
