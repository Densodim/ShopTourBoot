package com.shoptourr.idempotency

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class RepeatableBodyRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

	val body: ByteArray = request.inputStream.readAllBytes()

	override fun getInputStream(): ServletInputStream = BytesServletInputStream(body)

	override fun getReader(): BufferedReader =
		BufferedReader(InputStreamReader(getInputStream(), characterEncoding ?: Charsets.UTF_8.name()))

	override fun getContentLength(): Int = body.size

	override fun getContentLengthLong(): Long = body.size.toLong()
}

private class BytesServletInputStream(bytes: ByteArray) : ServletInputStream() {

	private val input = ByteArrayInputStream(bytes)

	override fun read(): Int = input.read()

	override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)

	override fun isFinished(): Boolean = input.available() == 0

	override fun isReady(): Boolean = true

	override fun setReadListener(readListener: ReadListener) {
		error("Async read is not supported")
	}
}
