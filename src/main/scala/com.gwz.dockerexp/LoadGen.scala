package com.gwz.dockerexp

import java.security.SecureRandom;
import java.math.BigInteger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Support {
	private val random = new SecureRandom()
	def genWord() = {
		new BigInteger(130, random).toString(32)
	}
	val name = genWord()
}

case class Mucho() {
	def go() {
		Future( while(true) cycle() )
	}
	def cycle() {
		val words = {
			for( i <- 1 to 10000000 ) yield Support.genWord()
		}.toList
		words.sortWith( (a,b) => a < b )
	}
}