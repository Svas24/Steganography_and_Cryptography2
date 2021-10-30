package cryptography

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.xor

fun hide() {
    println("Input image file:")
    val inFileName = readLine()!!
    println("Output image file:")
    val outFileName= readLine()!!
    if (!File(inFileName).exists()) {
        println("Can't read input file!")
        return
    }
    println("Message to hide:")
    val message = readLine()!!.toByteArray()
    val image = ImageIO.read(File(inFileName))
    println("Password:")
    val password = readLine()!!.toByteArray()
    if (image.height * image.width < (message.size + 3) * 8) {
        println("The input image is not large enough to hold this message.")
        return
    }
    ImageIO.write(inject(image, message, password), "png", File(outFileName))
    println("Message saved in $outFileName image.")
}

fun show() {
    println("Input image file:")
    val imageFileName = readLine()!!
    val image = ImageIO.read(File(imageFileName))
    println("Password:")
    val password = readLine()!!.toByteArray()
    println("Message:")
    println(extract(image, password).toString(Charsets.UTF_8))
}

fun inject(image: BufferedImage, msg: ByteArray, pass: ByteArray): BufferedImage {
    var code = msg.onEachIndexed{ i, byte -> msg[i] =  byte.xor(pass[i % pass.size])}
    code += byteArrayOf(0, 0, 3)
    for (bitCnt in 0 until code.size * 8) {
        val x = bitCnt % image.width
        val y = bitCnt / image.width
        image.setRGB(x, y, image.getRGB(x, y) and 1.inv())                      // pixel bit = 0
        if (code[bitCnt / 8].toInt().and(0x80.ushr(bitCnt % 8)) != 0)           // if code bit == 1
            image.setRGB(x, y, (image.getRGB(x, y).inc()))                      // pixcel bit = 1
    }
    return image
}

fun extract(image: BufferedImage, pass: ByteArray): ByteArray {
    val code = mutableListOf<Byte>(0)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val bitCnt = y * image.width + x
            code[code.lastIndex] = (code.last() * 2).toByte()                   // code bit = 0 (shl)
            if (image.getRGB(x, y).and(1) != 0)                                 // if pixel bit == 1
                code[code.lastIndex] = code.last().inc()                        // code bit = 1

            if (bitCnt % 8 == 7) {                                              // byte complete
                if (code.size >= 3                                              // check for ...00 00 03
                    && (code[code.lastIndex - 2] * 256 + code[code.lastIndex - 1]) * 256 + code.last() == 3) {
                    code.removeAt(code.lastIndex - 2)
                    return code.onEachIndexed{ i, byte ->  code[i] = byte.xor(pass[i % pass.size])}.toByteArray()
                } else code.add(0)                                              // next byte
            }
        }
    }
    return ByteArray(0)                                                         // no data in image
}

fun main() {
    while (true) {
        println("Task (hide, show, exit):")
        when (val input = readLine()!!) {
            "exit" -> break
            "show" -> show()
            "hide" -> hide()
            else -> println("Wrong task: $input")
        }
    }
    println("Bye!")
}