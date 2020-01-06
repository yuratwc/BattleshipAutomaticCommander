import java.awt.Button
import java.awt.Font
import java.awt.Label
import java.lang.Exception
import javax.swing.*
import kotlin.system.exitProcess

fun startProcess (mom : Window) {
    val dr1 = JOptionPane.showInputDialog(mom, "あなたは先攻ですか？", "", 0, null,arrayOf("先攻", "後攻"), null)
    if(dr1 == null) {
        exitProcess(0)
        return
    }
    if(dr1 is String) {
        if(dr1 == "先攻") {
            mom.isFirstStrike = true
        }
    }

    mainLoop(mom)
}

fun mainLoop(mom : Window) {
    var turn = 1
    if(mom.isFirstStrike) {
        turn = 0
    }

    while(true) {
        if(turn % 2 == 0) {
            //attack
            try {
                val cmd = mom.showAttackAction()
                val wnd = CommandAttackWindow(cmd.toLabelString(), cmd is CommandMove)
                wnd.isVisible = true
                wait(wnd)
                wnd.isVisible = false
                mom.receiveAttackActionResult(wnd.result)
            } catch (e: Exception){}
        } else {
            //receive
            try{
                val wnd = CommandAcceptWindow()
                wnd.isVisible = true
                wait(wnd)
                wnd.isVisible = false
                val aiteStr = mom.acceptEnemysAction(wnd.result)

                if(aiteStr != "") {
                    val win2 = CommandResultWindow(aiteStr)
                    win2.isVisible = true
                    wait(win2)
                    win2.isVisible = false
                }

            } catch (e: Exception){}

        }
        turn++
    }
}

fun wait(w : Waiter) {
    while(!w.check()) {
        try {
            Thread.sleep(100)
        }catch(e : Exception) {}
    }
}

interface Waiter {
    fun check() : Boolean
}

class CommandAttackWindow(str : String, move : Boolean) : JFrame("指示"), Waiter{

    private val combo = JComboBox<String>()

    var result = "miss"

    private var exit = false

    override fun check() : Boolean {
        return exit
    }

    init {
        setSize(800, 400)
        isAlwaysOnTop = true
        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

        layout = null
        val label = Label(str)
        label.setBounds(20, 20, 400, 80)
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        add(label)


        val btn = Button("決定")
        btn.setBounds(400, 180, 200, 40)
        add(btn)

        if(!move) {
            val label2 = Label("結果を入力してください：")
            label2.setBounds(20, 100, 400, 80)
            label2.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
            add(label2)

            combo.addItem("ハズレ")
            combo.addItem("命中")
            combo.addItem("命中！撃沈！")
            combo.addItem("波高し")
            combo.setBounds(20, 180, 300, 40)
            add(combo)

            btn.addActionListener {

                if(combo.selectedItem != null && combo.selectedItem is String) {
                    val txt = combo.selectedItem as String
                    if(txt == "ハズレ") {
                        result = "miss"
                    }
                    if(txt == "命中！撃沈！") {
                        result = "hit sinking"
                    }
                    if(txt == "命中") {
                        result = "hit"
                    }
                    if(txt == "波高し") {
                        result = "highwave"
                    }
                    exit = true
                }
            }
        } else {
            btn.addActionListener {
                    exit = true
            }
        }
    }
}

class CommandAcceptWindow() : JFrame("相手の指示を待っています。"), Waiter{

    private val pos1x = JComboBox<String>()
    private val pos1y = JComboBox<String>()

    private val pos2a = JComboBox<String>()
    private val pos2b = JComboBox<String>()

    var result = Command()
    private var exit = false

    override fun check() : Boolean {
        return exit
    }

    init {
        setSize(800, 400)
        isAlwaysOnTop = true

        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        layout = null
        val label = Label("相手の行動結果を入力してください")
        label.setBounds(20, 20, 400, 40)
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        add(label)


        val label2 = Label("攻撃：")
        label2.setBounds(20, 60, 400, 40)
        label2.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        add(label2)

        pos1x.setBounds(20, 100, 50, 40)
        for(i in 'A'..'E')
            pos1x.addItem(i.toString())
        add(pos1x)


        pos1y.setBounds(90, 100, 50, 40)
        for(i in 1..5)
            pos1y.addItem(i.toString())
        add(pos1y)


        val btn1 = Button("決定")
        btn1.setBounds(400, 100, 200, 40)
        add(btn1)

        btn1.addActionListener {
            val x = (pos1y.selectedItem as String).toInt() - 1
            val y = ((pos1x.selectedItem as String)[0] - 'A')
            result = CommandAttack(x, y)
            exit = true
        }


        val label3 = Label("移動：")
        label3.setBounds(20, 180, 400, 40)
        label3.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        add(label3)


        pos2a.setBounds(20, 220, 50, 40)
        pos2a.addItem("北")
        pos2a.addItem("南")
        pos2a.addItem("西")
        pos2a.addItem("東")
        add(pos2a)

        pos2b.setBounds(90, 220, 50, 40)
        pos2b.addItem("1")
        pos2b.addItem("2")
        add(pos2b)


        val btn2 = Button("決定")
        btn2.setBounds(400, 220, 200, 40)
        add(btn2)

        btn2.addActionListener{
            val dirstr = (pos2a.selectedItem as String)
            val dist = (pos2b.selectedItem as String).toInt()

            when(dirstr) {
                "東"->{
                    result = CommandMove(0, dist, 0)
                }
                "西"->{
                    result = CommandMove(0, -dist, 0)
                }
                "北"->{
                    result = CommandMove(0, 0, -dist)
                }
                "南"->{
                    result = CommandMove(0, 0, dist)
                }
            }
            exit = true
        }
    }
}

class CommandResultWindow(str : String) : JFrame("指示"), Waiter{

    private var exit = false

    override fun check() : Boolean {
        return exit
    }

    init {
        setSize(800, 300)
        layout = null
        isAlwaysOnTop = true
        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE


        val label = Label( getLocalizedString(str))
        label.setBounds(20, 20, 380, 40)
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
        add(label)

        val btn = Button("次へ")
        btn.setBounds(400, 20, 200, 40)
        add(btn)
        btn.addActionListener {
            exit = true
        }

    }

    private fun getLocalizedString(str : String) : String {

        when(str) {
            "miss"->{
                return "ハズレ！"
            }
            "highwave"->{
                return "波高し！"
            }
            "hit"->{
                return "命中！"
            }
            "hit sinking"->{
                return "命中！撃沈！"
            }
        }
        return str
    }
}
