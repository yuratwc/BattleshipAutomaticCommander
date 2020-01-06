import java.awt.*
import javax.swing.*;

class Window(private val myOcean : Ocean, private val enemyOcean : Ocean) : JFrame("Battleship Automatic Commander") {
    private val myCommander : Commander = Commander(myOcean)
    private val enemyCommander : Commander = Commander(enemyOcean)

    var isFirstStrike : Boolean = false

    private val enemyAI = AIRandom(enemyOcean)
    private val userAI = AIUser(myOcean)

    private var redraws = emptyArray<Component>()
    init {
        setSize(1000, 600)
        layout = null
        defaultCloseOperation = EXIT_ON_CLOSE



        val rect1 = MapRect(enemyOcean)
        rect1.setBounds(30, 30, 400, 400)
        add(rect1)

/*
        val rect2 = PredictRect(myOcean, userAI.predict)
        rect2.setBounds(480, 30, 400, 400)
        add(rect2)
*/
        val rect3 = PredictRect(enemyOcean, enemyAI.map)
        rect3.setBounds(480, 30, 400, 400)
        add(rect3)
        redraws = arrayOf(rect1, rect3)

        /*
        val inputBox = TextField()
        inputBox.font = Font(Font.SANS_SERIF, Font.PLAIN, 20)
        inputBox.setBounds(30, 480, 600, 30)
        add(inputBox)

        val btn = Button("Command")
        btn.setBounds(680, 480, 120, 30)



        btn.addActionListener { e ->
            if(!inputBox.text.isBlank()) {
                try {
                    userAI.msgUpdate(inputBox.text)
                    val userCommand = userAI.command()
                    println("User -> \"$userCommand\"");
                    if(myCommander.validCommand(userCommand)) {
                        val q = enemyAI.accept(userCommand.toString())
                        userAI.receive(q)
                    }
                    rect1.repaint()
                    rect2.repaint()
                    rect3.repaint()

                    Thread.sleep(500)
                    val enemyCommand = enemyAI.command()
                    println("Enemy -> \"$enemyCommand\"")
                    if(enemyCommander.validCommand(enemyCommand)){
                        val q = userAI.accept(enemyCommand.toString())
                        enemyAI.receive(q)
                    }
                    rect1.repaint()
                    rect2.repaint()
                    rect3.repaint()

                } catch (e: BattleshipException) {
                    JOptionPane.showMessageDialog(this, e.message);
                }
                inputBox.text = ""
            }
        }
        add(btn)
        */
        setVisible(true)
        startProcess(this)
    }


    fun showAttackAction() : Command {
        val enemyCommand = enemyAI.command()
        if(enemyCommander.validCommand(enemyCommand)) {
            return enemyCommand
        }
        println("Invalid Command" + enemyCommand)
        return enemyCommand
    }

    fun receiveAttackActionResult(str : String) {
        enemyAI.receive(str)
        for(c in redraws)
            c.repaint()
        //val enemyCommand = enemyAI.command()
        //return enemyCommand
    }

    fun acceptEnemysAction(cmd : Command) : String{
        val k = enemyAI.accept(cmd.toString())
        for(c in redraws)
            c.repaint()
        return k
    }
}

internal class MapRect(private val ocean : Ocean) : Canvas() {
    override fun paint(g: Graphics) {

        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        val cellSize = height / (ocean.height + 1);
        for(i in 0 until (ocean.height + 2)) {
            g.drawLine(0, i * cellSize, width, i * cellSize)
            g.drawLine(i * cellSize,0,  i * cellSize, height)

            if(i in 1..ocean.height) {
                g.drawString(('1' + (i - 1)).toString(), i * cellSize + cellSize / 2, cellSize / 2)
                g.drawString(('A' + (i - 1)).toString(), cellSize / 2, i * cellSize + cellSize / 2)
            }

        }

        //draw ships
        for(i in 0 until ocean.ships.size) {
            val ship = ocean.ships[i]
            g.drawString("[${i}]", (ship.x+1) * cellSize + cellSize / 6, (ship.y+1) * cellSize + cellSize / 3)
            g.drawString("HP:${ship.hp}", (ship.x+1) * cellSize + cellSize / 6, (ship.y+1) * cellSize + cellSize - 10)
        }
    }
}

internal class PredictRect(private val ocean : Ocean, val map : IntArray) : Canvas() {
    override fun paint(g: Graphics) {

        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        val cellSize = height / (ocean.height + 1);


        for(y in 0 until ocean.height) {
            for(x in 0 until ocean.width) {
                val state = map[y * ocean.width + x]
                val left  = cellSize * (x + 1)
                val top = cellSize * (y + 1)
                when(state) {
                    -1 -> {
                        g.color = Color.BLACK
                        g.drawLine(left, top, left + cellSize, top + cellSize)
                    }
                    else -> {
                        if(state >= -1) {
                            if (state != 0) {
                                g.color = Color(
                                    255,
                                    Math.max(Math.min(255, 255 - state), 0),
                                    Math.max(Math.min(255, 255 - state), 0)
                                )
                                g.fillRect(left, top, cellSize, cellSize)
                            }
                        }
                    }
                }
                g.color = Color.BLACK
                g.drawString(state.toString(), left + cellSize / 2, top + cellSize / 2)
            }
        }

        g.color = Color.BLACK
        for(i in 0 until (ocean.height + 2)) {
            g.drawLine(0, i * cellSize, width, i * cellSize)
            g.drawLine(i * cellSize,0,  i * cellSize, height)

            if(i in 1..ocean.height) {
                g.drawString(('1' + (i - 1)).toString(), i * cellSize + cellSize / 2, cellSize / 2)
                g.drawString(('A' + (i - 1)).toString(), cellSize / 2, i * cellSize + cellSize / 2)
            }

        }

    }
}

