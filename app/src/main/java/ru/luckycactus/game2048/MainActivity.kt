package ru.luckycactus.game2048

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.luckycactus.game.Game
import ru.luckycactus.game2048view.GameView

class MainActivity : AppCompatActivity() {

    private val game = Game(4, 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            override fun onSwipeRight() {
                slide(Game.RIGHT)
            }

            override fun onSwipeLeft() {
                slide(Game.LEFT)
            }

            override fun onSwipeTop() {
                slide(Game.TOP)
            }

            override fun onSwipeBottom() {
                slide(Game.BOTTOM)
            }
        })

        btnRestart.setOnClickListener {
            game.restart()
            gameView.update()
        }

        gameView.game = game
        game.start()
        gameView.update()
    }

    private fun slide(direction: Int) {
        if (game.isDirectionSlidable(direction)) {
            game.slide(direction)
            gameView.update()
        }
    }
}