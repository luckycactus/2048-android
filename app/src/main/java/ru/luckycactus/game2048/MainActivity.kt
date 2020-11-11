package ru.luckycactus.game2048

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import ru.luckycactus.game2048.model.Game

class MainActivity : AppCompatActivity() {

    private val game = Game(4, 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clickListener = { view: View ->
            val direction = when (view.id) {
                R.id.btnLeft -> Game.LEFT
                R.id.btnTop -> Game.TOP
                R.id.btnRight -> Game.RIGHT
                R.id.btnBottom -> Game.BOTTOM
                else -> throw Exception()
            }
            game.slide(direction)
            render()
        }

        findViewById<Button>(R.id.btnLeft).setOnClickListener(clickListener)
        findViewById<Button>(R.id.btnTop).setOnClickListener(clickListener)
        findViewById<Button>(R.id.btnRight).setOnClickListener(clickListener)
        findViewById<Button>(R.id.btnBottom).setOnClickListener(clickListener)

        game.start()
        render()
    }

    private fun render() {
        val sb = StringBuilder()
        game.grid.forEach { row ->
            row.joinTo(sb, " ", postfix = "\n", transform = { "%3d".format(it?.value ?: 0) })
        }
        findViewById<TextView>(R.id.tvGame).text = sb.toString()
    }
}