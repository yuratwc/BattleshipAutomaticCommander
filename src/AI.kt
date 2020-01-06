import kotlin.collections.ArrayList

abstract class AIBase(protected val ocean : Ocean) {
    abstract fun command() : Command

    abstract fun receive(str : String)

    protected open fun acceptEvent(cmd : Command) {}

    fun accept(msg : String) : String {
        val cmd = CommandConverter(msg).command
        acceptEvent(cmd)
        if(cmd is CommandAttack) {
            var highWave = false
            for(ship in ocean.ships) {
                if(ship.x == cmd.x && ship.y == cmd.y) {
                    ship.hp--;
                    if(ship.hp <= 0) {
                        return "hit sinking"
                    }
                    return "hit"
                }
                if(Math.abs(ship.x - cmd.x) <= 1 && Math.abs(ship.y - cmd.y) <= 1) {
                    highWave = true
                }
            }
            if(highWave) {
                return "highwave"
            }
        }
        return ""
    }
}

class AIRandom(private val oce : Ocean) : AIBase(oce) {
    private val hitQueue = ArrayList<Pos>()

    private val noShipQueue = ArrayList<Pos>()
    private val sunkenShips = ArrayList<Pos>()

    val map = IntArray(ocean.width * ocean.height)
    private var lastCommand = Command()
    init {
        for(i in 0 until map.size) {
            map[i] = 64
        }
    }


    private fun genRound4Pos(centre : Pos = Pos(0,0)) : Sequence<Pos>{
        return sequence{
            yield(Pos(-1 + centre.x, 0 + centre.y))
            yield(Pos(0 + centre.x, -1 + centre.y))
            yield(Pos(0 + centre.x, 1 + centre.y))
            yield(Pos(1 + centre.x, 0 + centre.y))
        }
    }

    private fun genRound8Pos(centre : Pos = Pos(0,0)) : Sequence<Pos>{
        return sequence{
            for(y in -1..1) {
                for(x in -1..1) {
                    if(x == 0 && y == 0)
                        continue
                    yield(Pos(x + centre.x, y + centre.y))
                }
            }
        }
    }

    private fun randomMove() : Command {
        println("Fire! RandomMove!")

        val movableMap = BooleanArray(ocean.width * ocean.height)
        for(ship in ocean.ships) {
            movableMap[ship.y * ocean.width + ship.x] = true
        }

        for(ship in ocean.ships) {
            if(ship.hp <= 0 )
                continue

            genRound4Pos(Pos(ship.x, ship.y)).forEach {
                if(!movableMap[it.y * ocean.width + it.x]) {
                    return CommandMove(ship.shipId, it.x - ship.x, it.y - ship.y)
                }
            }
        }
        //gameover
        return CommandMove(0,0,1)
    }

    override fun command() : Command {
        var allMinus = true
        for(va in map) {
            if(va != -1) {
                allMinus = false
                break
            }
        }

        if(allMinus) {
            for(i in 0 until map.size) {
                map[i] = 64
            }
            sunkenShips.clear()
        }

        if(hitQueue.size > 0) {
            //yuusen de ateru
            lastCommand = CommandAttack(hitQueue[0].x, hitQueue[0].y)
            return lastCommand
        }

        val allPos = ArrayList<Pos>(ocean.width * ocean.height + 20)
        for(y in 0 until ocean.height) {
            for(x in 0 until ocean.width) {
                val p = Pos(x, y)
                if(!sunkenShips.contains(p))
                    allPos.add(p)
            }
        }
        allPos.sortByDescending {map[it.y *  ocean.width + it.x]}

        val poses = ArrayList<Pos>()
        for(ship in ocean.ships) {
            if(ship.hp <= 0)
                continue
            val pos = Pos(ship.x, ship.y)

            genRound8Pos(pos).forEach {
                if(it.inArea(ocean.width, ocean.height) && map[it.y * ocean.width + it.x] >= 0 && !sunkenShips.contains(it))
                    poses.add(it)
            }
        }
        poses.sortByDescending { map[it.y * ocean.width + it.x] }

        if(poses.size > 0 && map[poses[0].y * ocean.width + poses[0].x] >= map[allPos[0].y * ocean.width + allPos[0].x]) {
            lastCommand = CommandAttack(poses[0].x, poses[0].y)
            return lastCommand
        }
        //bfs
        //val bfs = Stack<Pos>()
        //val bfsCost = Stack<Int>()

        val highPos = allPos[0]

        //push nearest ship pos
        val movableMap = BooleanArray(ocean.width * ocean.height)
        for(ship in ocean.ships) {
            movableMap[ship.y * ocean.width + ship.x] = true
        }
        var nearestShip = ocean.ships[0]

        println("target$highPos")
        val nearestIgnore = getNearestShip(highPos, false)
        val nearestConsidered = getNearestShip(highPos, true)

        println("checkit")
//        var escapeMode = nearestIgnore === nearestConsidered
        var escapeMode = false

        if(nearestConsidered.x == highPos.x && nearestConsidered.y == highPos.y) {
            //ship on attack cell
            //var canMove = false
            genRound4Pos(poses[0]).forEach {
                for(scale in 2 downTo 1) {
                    val newPos = Pos(it.x * scale, it.y * scale)
                    if(newPos.inArea(ocean.width, ocean.height) && movableMap[newPos.y * ocean.width + newPos.x]) {
                        //canMove = true
                        lastCommand = CommandMove( nearestConsidered.shipId, newPos.x - highPos.x, newPos.y - highPos.y)
                        return lastCommand
                    }
                }

            }

            escapeMode = true
        }

        if(escapeMode) {
            //mode sakeru
            println("sakeruze")
            lastCommand = randomMove()
            return lastCommand
        }
        println("begin bfs")

        val bfsMap = IntArray(movableMap.size)
        for(i in 0 until movableMap.size)
            bfsMap[i] = if (movableMap[i]) -1 else 0

        var bfsMinCost = 999999
        var bfsMinPos = Pos(0,0)
        var bfsMinRoute = IntArray(movableMap.size)
        var bfsMinShip = ocean.ships[0]

        for(ship in ocean.ships){
            if(ship.hp <= 0)
                continue
            bfsMap[ship.y * ocean.width + ship.x] = 0
            val route = startBFS(Pos(ship.x, ship.y), bfsMap, true, ocean.width, ocean.height)
            bfsMap[ship.y * ocean.width + ship.x] = -1

            genRound4Pos(allPos[0]).forEach {
                if(it.inArea(ocean.width, ocean.height) && route[it.y * ocean.width + it.x] < bfsMinCost) {
                    bfsMinCost = route[it.y * ocean.width + it.x]
                    bfsMinPos = it
                    bfsMinRoute = route
                    bfsMinShip = ship
                }
            }
        }

        if(bfsMinCost >= 99999) {
            println("MinCost! Occured!")

            lastCommand = randomMove()
            return lastCommand
        }

        println("check bfs costs")

        while(bfsMinCost > 1) {
            var breakFlag = false
            for (it in genRound4Pos(bfsMinPos)) {
                for(scale in 2 downTo 1) {
                    val newPos = Pos(it.x * scale, it.y * scale)
                    if(newPos.inArea(ocean.width, ocean.height) && bfsMinCost - scale == bfsMinRoute[newPos.y * ocean.width + newPos.x]) {
                        bfsMinPos = newPos
                        bfsMinCost -= scale
                        breakFlag = true
                        break
                    }
                }
                if(breakFlag) break
            }

        }
        val curPos = bfsMinPos
        nearestShip = bfsMinShip
        println("bfsend:" + CommandMove(nearestShip.shipId, curPos.x - nearestShip.x, curPos.y - nearestShip.y))

        if((curPos.x - nearestShip.x == 0 && curPos.y - nearestShip.y == 0) || (Math.abs(curPos.x - nearestShip.x) + Math.abs(curPos.y - nearestShip.y)) > 2) {
            println("Error! Occured!")
            lastCommand = randomMove()
            return lastCommand
        }
        lastCommand = CommandMove(nearestShip.shipId, curPos.x - nearestShip.x, curPos.y - nearestShip.y)
        return lastCommand
    }

    private fun getNearestShip(pos : Pos, checkMovable : Boolean) : Ship {
        val movableMap = BooleanArray(ocean.width * ocean.height)
        for(ship in ocean.ships) {
            movableMap[ship.y * ocean.width + ship.x] = true
        }
        var minDistance = 999
        var nearestShip = ocean.ships[0]
        for(ship in ocean.ships) {
            val distance = (pos.x - ship.x)*(pos.x - ship.x)+ (pos.y - ship.y) * (pos.y - ship.y)
            if(distance < minDistance) {
                //check can move
                if(checkMovable) {
                    var canMove = true
                    for (y in -1..1) {
                        for (x in -1..1) {
                            if (x == 0 && y == 0) continue
                            val p = Pos(x + ship.x, y + ship.y)
                            if (!p.inArea(ocean.width, ocean.height)) continue
                            if (canMove)
                                canMove = movableMap[p.y * ocean.width + p.x]
                        }
                    }
                    if (!canMove) continue;
                }

                minDistance = distance
                nearestShip = ship
            }
        }

        return nearestShip
    }

    private fun startBFS(startPos : Pos, map : IntArray, trans : Boolean, width : Int, height : Int) : IntArray {
        val costMap = IntArray(width * height)
        for(i in 0 until costMap.size)
            costMap[i] = 999999
        bfs(startPos, 0, costMap, map, trans, width, height)

        //for debugging
        println("-----");
        for(y in 0 until height) {
            for(x in 0 until width)
                print(" ${costMap[y * width + x]}")
            println()
        }

        return costMap
    }

    private fun bfs(pos : Pos, cost : Int, costMap : IntArray, map : IntArray, trans : Boolean, w : Int, h : Int) {
        if(!pos.inArea(w, h))
            return
        val index = pos.y * w + pos.x
        if(trans && map[index] == -1)
            return

        if(cost >= costMap[index])
            return
        costMap[index] = cost

        genRound4Pos(pos).forEach {
            bfs(it, cost + 1, costMap, map, trans, w, h)
        }

    }

    override fun receive(str: String) {
        if(lastCommand is CommandAttack) {
            receiveAttack(lastCommand as CommandAttack, str)
        }
        if(lastCommand is CommandMove) {
            receiveMove(lastCommand as CommandMove, str)
        }
    }

    private fun receiveMove(cmd : CommandMove, str : String) {
        //none;
    }

    override fun acceptEvent(cmd : Command) {
        if(cmd is CommandMove) {
            //aite ga idou sita
            val newMap = IntArray(map.size)

            for(y in 0 until ocean.height) {
                for(x in 0 until ocean.width) {
                    val p = Pos(x, y)
                    //val ap = Pos(x + cmd.offsetX, y + cmd.offsetY)
                    val bp = Pos(x - cmd.offsetX, y - cmd.offsetY)

                    if(!bp.inArea(ocean.width, ocean.height)) {
                        newMap[p.y * ocean.width + p.x] = map[p.y * ocean.width + p.x]
                        continue
                    }
                    if(map[bp.y * ocean.width + bp.x] == -1) {
                        if(map[p.y * ocean.width + p.x] == -1) {
                            newMap[p.y * ocean.width + p.x] = -1
                        }else {
                            newMap[p.y * ocean.width + p.x] = map[p.y * ocean.width + p.x]
                        }
                        continue
                    }
                    if(map[p.y * ocean.width + p.x] == -1) {
                        newMap[p.y * ocean.width + p.x] = 64
                        continue
                    }
                    newMap[p.y * ocean.width + p.x] = map[p.y * ocean.width + p.x]
                }
            }

            for(y in 0 until ocean.height) {
                for(x in 0 until ocean.width) {
                    val p = Pos(x, y)
                    map[p.y * ocean.width + p.x] = newMap[p.y * ocean.width + p.x]
                }
            }

            for(ship in sunkenShips) {
                if(Pos(ship.x, ship.y).inArea(ocean.width, ocean.height))
                    map[ship.y * ocean.width + ship.x] = -1
            }

        } else if(cmd is CommandAttack) {
            for(y in -1..1) {
                for (x in -1..1) {
                    val pos = Pos(x + cmd.x, y + cmd.y)
                    if (pos.inArea(ocean.width, ocean.height) && map[pos.y * ocean.width + pos.x] != -1)
                        map[pos.y * ocean.width + pos.x] += 5
                }
            }
            map[cmd.y * ocean.width + cmd.x] = -1
            //print("nyaaa~")
        }
    }

    private fun receiveAttack(cmd : CommandAttack, str : String) {
        if(!Pos(cmd.x, cmd.y).inArea(ocean.width, ocean.height))
            return

        when(str) {
            "miss" -> {
                for(y in -1..1) {
                    for (x in -1..1) {
                        val pos = Pos(x + cmd.x, y + cmd.y)
                        if (pos.inArea(ocean.width, ocean.height))
                            map[pos.y * ocean.width + pos.x] = -1
                    }
                }

                val centre = Pos(cmd.x, cmd.y)
                if(hitQueue.contains(centre)) {
                    hitQueue.remove(centre)
                }

                map[cmd.y * ocean.width + cmd.x] = -1
            }
            "hit" -> {
                hitQueue.add(Pos(cmd.x, cmd.y))
                map[cmd.y * ocean.width + cmd.x] = 255
            }
            "hit sinking"-> {
                hitQueue.remove(Pos(cmd.x, cmd.y))
                sunkenShips.add(Pos(cmd.x, cmd.y))
                map[cmd.y * ocean.width + cmd.x] = -1
            }
            "highwave" -> {
                for(y in -1..1) {
                    for (x in -1..1) {
                        val pos = Pos(x + cmd.x, y + cmd.y)
                        if (pos.inArea(ocean.width, ocean.height) && map[pos.y * ocean.width + pos.x] != -1) {
                            //println("HighWaveSet ${ map[pos.y + ocean.width + pos.x]} -> ${ map[pos.y + ocean.width + pos.x] + 5}")
                            map[pos.y * ocean.width + pos.x] += 5
                        } else {
                            //println("Skip ${pos.x} , ${pos.y}")
                        }
                    }
                }
                map[cmd.y * ocean.width + cmd.x] = -1


                val centre = Pos(cmd.x, cmd.y)
                if(hitQueue.contains(centre)) {
                    hitQueue.remove(centre)
                }

            }
        }
    }
}

class AIUser(private val oce : Ocean) : AIBase(oce) {

    private var msg : String? = null
    private var lastCommand = Command()

    val predict = IntArray(ocean.width * ocean.height)

    fun msgUpdate(str : String) {
        msg = str
    }

    override fun command() : Command {
        while(true) {
            Thread.sleep(10)
            if(msg != null) {
                val r : String = msg!!
                val cmd = CommandConverter(r)
                msg = null
                lastCommand = cmd.command
                return lastCommand
            }
        }
    }
    override fun receive(str: String) {
        if(lastCommand !is CommandAttack)
            return
        val cmd = lastCommand as CommandAttack
        when(str) {
            "miss" -> {
                predict[cmd.y * ocean.width + cmd.x] = -1
            }
            "hit" -> {
                //hitQueue.add(Pos(cmd.x, cmd.y))
                predict[cmd.y * ocean.width + cmd.x] = 255

            }
            "hit sinking"-> {
                var r = Pos(-1, -1)
                    /*
                for(que in hitQueue) {
                    if(que.x == cmd.x && que.y == cmd.y) {
                        r =  que
                    }
                }
                if(r.x != -1 && r.y != -1) {
                    hitQueue.remove(r)
                }
                */
                predict[cmd.y * ocean.width + cmd.x] = -1
            }
            "highwave" -> {
                for(y in -1..1) {
                    for(x in -1..1) {
                        val pos = Pos(x + cmd.x, y + cmd.y)
                        if(pos.inArea(ocean.width, ocean.height))
                            predict[pos.y * ocean.width + pos.x] += 5
                    }
                }
            }
        }
    }


}

open class Command {

    open fun toLabelString() : String {
        return ""
    }
}

class CommandMove(val shipId : Int, val offsetX : Int, val offsetY: Int) : Command() {
    override fun toString(): String {
        return "move $shipId $offsetX $offsetY"
    }


    override fun toLabelString() : String {
        var dir = ""
        if(offsetX > 0) {
            dir = "東"
        }
        if(offsetX < 0) {
            dir = "西"
        }
        if(offsetY > 0) {
            dir = "南"
        }
        if(offsetY < 0) {
            dir = "北"
        }
        val cnt = Math.abs(offsetX) + Math.abs(offsetY)
        return "[移動] 潜水艦を $dir に $cnt マス移動！"
    }
}
class CommandAttack(val x : Int, val y: Int) : Command() {
    override fun toString(): String {
        return "attack $x $y"
    }

    override fun toLabelString() : String {
        val labels = arrayOf("A", "B", "C", "D", "E")
        var ypos = "(指定不能->$y)"

        if(y < labels.size) {
            ypos = labels[y]
        }
        val xpos = x + 1
        return "[攻撃] [$ypos-$xpos]に魚雷発射！"
    }
}

class CommandConverter(private val commandStr : String) {
    val command : Command;
    init {
        val arys = commandStr.split(" ")

        when(arys[0]){
            "attack" -> {
                var x : Int
                var y : Int
                if(arys.size == 3) {
                    x = arys[1].toInt()
                    y = arys[2].toInt()
                } else if(arys.size == 2) {
                    val pos = Pos(arys[1])
                    x = pos.x
                    y = pos.y
                } else {
                    throw BattleshipException("attackコマンドの引数が異なります。-> \"attack :x :y\"")
                }

                command = CommandAttack(x, y)
            }
            "move" -> {
                val q = arys[1].toInt()
                var x : Int
                var y : Int
                if(arys.size == 4) {
                    x = arys[2].toInt()
                    y = arys[3].toInt()
                } else if(arys.size == 3) {
                    val pos = Pos(arys[2])
                    x = pos.x
                    y = pos.y
                } else {
                    throw BattleshipException("moveコマンドの引数が異なります。-> \"move :ship_id :x :y\"")
                }
                command = CommandMove(q, x, y)
            }
            else -> throw BattleshipException("コマンドが違います。")
        }
    }
}