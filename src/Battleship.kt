import java.lang.Exception
import java.util.*;

fun main(args: Array<String>) {
    val myOcean = Ocean(5, 5)
    val enemyOcean = Ocean(5, 5)

    val win = Window(myOcean, enemyOcean)
}

fun getPos(str:String) : Pos {
    if (str.length < 2 || !(str.length == 2 || str.contains("-"))) {
        throw BattleshipException("座標の指定の仕方が違います")
    }

    var bef = str[0]
    var aft = str[1]
    if (str.contains("-")) {
        val sp = str.split("-")
        bef = sp[0][0]
        aft = sp[1][0]
    }
    var x : Int
    var y : Int
    if (bef in '1'..'9') {
        x = bef - '1'
        y = aft - 'A'
    } else {
        x = aft - '1'
        y = bef - 'A'
    }
    return Pos(x, y)
}
class Ocean (val width : Int, val height: Int){
     val ships : Array<Ship> = arrayOf(Ship(this, 0), Ship(this, 1), Ship(this, 2), Ship(this, 3))


    init {
        locateShip()
    }

    private fun locateShip() {
        var rand = Random()
        val locateMap = BooleanArray(width * height)

        for(ship in ships) {
            var x : Int
            var y : Int
            while(true) {
                x = rand.nextInt(width)
                y = rand.nextInt(height)
                if(!locateMap[y * width + x])
                    break
            }
            locateMap[y * width + x] = true
            ship.x = x
            ship.y = y
        }
    }

}

class Ship(val ocean : Ocean, val shipId : Int) {
    var x : Int = 0
    var y : Int = 0
    var hp : Int = 3

    fun canMove(pos : Pos) : Boolean {
        if(hp <= 0) {
            return false
        }

        return (Math.abs(x - pos.x) <= 2 && Math.abs(y - pos.y) == 0) || (Math.abs(y - pos.y) <= 2 && Math.abs(x - pos.x) == 0)
    }
}

class Commander(private val myOcean : Ocean) {

    fun validCommand(command : Command) : Boolean {

        if(command is CommandAttack) {
            val x = command.x
            val y = command.y

            if(!(x in 0 until myOcean.width) || !(y in 0 until myOcean.height)) {
                throw BattleshipException("範囲外に攻撃しようとしました。-> \"attack :x :y\"")
            }

            var attackable = false;
            for(ship in  myOcean.ships) {
                if(Math.abs(ship.x - x) <= 1 && Math.abs(ship.y - y) <= 1 && !(ship.x == x && ship.y == y)){
                    attackable = true
                    break
                }
            }
            if(!attackable) {
                throw BattleshipException("その座標に攻撃できる船がありません。 -> \"move :ship_id :x :y\"")
            }

            /*
            for(ship in  enemyOcean.ships) {
                if(ship.x == x && ship.y == y) {
                    ship.hp --;
                } else if(Math.abs(ship.x - x) <= 1 && Math.abs(ship.y - y) <= 1 ){
                    //high wave
                }
            }
            */
            return true
        }
        if(command is CommandMove) {
            val x = command.offsetX
            val y = command.offsetY
            val q = command.shipId
            if(myOcean.ships.size <= q) {
                throw BattleshipException("存在しない船が選択されました。 -> \"move :ship_id :x :y\"")
            }

            val ship = myOcean.ships[q]

            if(ship.hp <= 0) {
                throw BattleshipException("動けない船が選択されました。 -> \"move :ship_id :x :y\"")
            }
            if(!(ship.x + x in 0 until myOcean.width && ship.y + y in 0 until myOcean.height)) {
                throw BattleshipException("範囲外に移動しようとしました。-> \"move :ship_id :x :y\"")
            }
            if(Math.abs(x) + Math.abs(y) > 2 || (Math.abs(x) > 0 && Math.abs(y) > 0)) {
                throw BattleshipException("船は2マス以上移動できません。 -> \"move :ship_id :x :y\"")
            }
            for(s in  myOcean.ships) {
                if(s != ship && s.x == (ship.x + x) && s.y == (ship.y + y)) {
                    throw BattleshipException("同じマスに船は2つ以上配置できません。 -> \"move :ship_id :x :y\"")
                }
            }
            ship.x += x
            ship.y += y

            return true
        }


        return false

    }
}

class Pos(val x : Int , val y: Int) {
    constructor(str: String) : this(getPos(str).x, getPos(str).y)

    fun inArea(w : Int, h : Int) : Boolean {
        return inArea(0, 0, w, h)
    }
    fun inArea(left : Int, top: Int, right: Int, bottom: Int) : Boolean {
        return x in left until right && y in top until bottom
    }

    override fun equals(other: Any?): Boolean {
        if(other is Pos) {
            return other.x == x && other.y == y
        }
        return false;
    }

    override fun hashCode(): Int {
        return y * 5 + x
    }

    override fun toString(): String {
        return "{$x, $y}"
    }
}

class BattleshipException (message : String) : Exception(message)