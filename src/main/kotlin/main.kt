import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Color
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


fun main() = runBlocking {
    println("Hello World!")
    val toolkit = Toolkit.getDefaultToolkit()
    val robot = Robot()

    val claimImage: BufferedImage = ImageIO.read(File("claim.png").inputStream())
    val confirmImage: BufferedImage = ImageIO.read(File("confirm.png").inputStream())
    val metaConfirmImage: BufferedImage = ImageIO.read(File("metaconfirm.png").inputStream())

    val screenImageFlow: Flow<BufferedImage> = flow {
        while (currentCoroutineContext().isActive) {
            val screenImage: BufferedImage = robot.createScreenCapture(Rectangle(toolkit.screenSize))
            emit(screenImage)
            delay(300)
        }
    }

    screenImageFlow.flatMapMerge { screenImage ->
        merge(
            screenImage.findSubImage(claimImage),
            screenImage.findSubImage(confirmImage),
            screenImage.findSubImage(metaConfirmImage)
        )
    }.distinctUntilChanged()
        .debounce(1_000)
        .collectLatest { (x, y) ->
            robot.clickMouse(x, y)
        }
}

private fun Robot.clickMouse(x: Int, y: Int) {
    mouseMove(x, y)
    delay(100)
    mousePress(InputEvent.BUTTON1_DOWN_MASK)
    mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
}

fun BufferedImage.findSubImage(subImage: BufferedImage): Flow<Pair<Int, Int>> {
    val firstSubPixel = subImage.getRGB(0, 0)
    println("first $firstSubPixel")
    println("first ${Color(firstSubPixel)}")
    /*
    .filter { (x, y) ->
        y < width - subImage.width
                && x < height - subImage.height
    }
     */
    return pixels().filter { (x, y) ->
        firstSubPixel == getRGB(x, y)
    }.filter { (x, y) ->
//        println("trying x$x y$y")
        subImage.pixels()
            .map { (sx, sy) ->
                val rgb = subImage.getRGB(sx, sy)
                val rgb1 = getRGB(sx + x, sy + y)
//                println("${Color(rgb)} == ${Color(rgb1)}")
                Color(rgb) == Color(rgb1)
            }.catch {
            }.count { !it } == 0
    }
}

fun BufferedImage.pixels(): Flow<Pair<Int, Int>> {
    val xFlow = (0 until width).asIterable().asFlow()
    val yFlow = (0 until height).asIterable().asFlow()
    return xFlow.flatMapConcat { x ->
        yFlow.map { y ->
            x to y
        }
    }
}
