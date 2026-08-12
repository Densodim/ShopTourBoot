package com.ShopTourBoot.ShopTourBoot

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<ShopTourBootApplication>().with(TestcontainersConfiguration::class).run(*args)
}
