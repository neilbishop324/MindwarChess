package com.neilb.mindwarchess

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.neilb.mindwarchess.adapter.MoveAdapter
import com.neilb.mindwarchess.databinding.ActivityMainBinding
import com.neilb.mindwarchess.game.Game
import com.neilb.mindwarchess.game.PositionList
import com.neilb.mindwarchess.interfaces.PromotionCallback
import com.neilb.mindwarchess.model.Ability
import com.neilb.mindwarchess.model.Move
import com.neilb.mindwarchess.model.Move.Companion.enPassant
import com.neilb.mindwarchess.model.Move.Companion.kill
import com.neilb.mindwarchess.model.Move.Companion.kingSideCastling
import com.neilb.mindwarchess.model.Move.Companion.move
import com.neilb.mindwarchess.model.Move.Companion.normal
import com.neilb.mindwarchess.model.Move.Companion.promotion
import com.neilb.mindwarchess.model.Move.Companion.queenSideCastling
import com.neilb.mindwarchess.model.PieceInPosition
import com.neilb.mindwarchess.model.PieceInPosition.Companion.bishop
import com.neilb.mindwarchess.model.PieceInPosition.Companion.isBlack
import com.neilb.mindwarchess.model.PieceInPosition.Companion.isWhite
import com.neilb.mindwarchess.model.PieceInPosition.Companion.king
import com.neilb.mindwarchess.model.PieceInPosition.Companion.knight
import com.neilb.mindwarchess.model.PieceInPosition.Companion.pawn
import com.neilb.mindwarchess.model.PieceInPosition.Companion.queen
import com.neilb.mindwarchess.model.PieceInPosition.Companion.rook
import com.neilb.mindwarchess.model.PossibleMove
import com.neilb.mindwarchess.ui.ChessView
import com.neilb.mindwarchess.ui.ChessView.Companion.enemyType
import com.neilb.mindwarchess.unit.compareArrayLists
import com.neilb.mindwarchess.unit.showAlertDialog
import com.neilb.mindwarchess.unit.toInt
import com.neilb.mindwarchess.unit.tryCatchAndLog
import kotlinx.coroutines.*
import java.util.UUID
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity(), PromotionCallback, OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var game: Game
    private lateinit var moves: ArrayList<Move>
    private lateinit var doubleMoves: ArrayList<Pair<Move, Move?>>
    private lateinit var moveAdapter: MoveAdapter
    private var lastPromoteData: Triple<Pair<Int, Int>, PieceInPosition?, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        game = Game()
        moves = arrayListOf()
        doubleMoves = arrayListOf()
        moveAdapter = MoveAdapter(doubleMoves)
        binding.movesList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.movesList.adapter = moveAdapter
        setClickListeners()
        binding.chessView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.chessView.onClick(
                    event.x.toInt(), event.y.toInt()
                ) { x, y ->
                    tryCatchAndLog { clickEventToTable(x, y) }
                }
            }
            true
        }
    }

    private fun setClickListeners() {
        binding.bBishop.setOnClickListener(this)
        binding.bQueen.setOnClickListener(this)
        binding.bKnight.setOnClickListener(this)
        binding.bRook.setOnClickListener(this)
        binding.wBishop.setOnClickListener(this)
        binding.wQueen.setOnClickListener(this)
        binding.wKnight.setOnClickListener(this)
        binding.wRook.setOnClickListener(this)
    }

    private fun clickEventToTable(x: Int, y: Int) {
        val positionStatus = binding.chessView.positionList[y][x].status
        val colorTypeList = binding.chessView.colorTypeList
        when {
            (game.whiteTurn && positionStatus == isWhite)
                    || (!game.whiteTurn && positionStatus == isBlack) -> clickEventToOwnPiece(x, y)
            game.inTarget && (positionStatus == PieceInPosition.isBlank) -> {
                //losing focus on targeting
                binding.chessView.clearColorList()
                game.inTarget = false
                game.targetPosition = null
            }
            game.inTarget && positionStatus == PieceInPosition.isTarget -> movePiece(x, y)
            game.inTarget && colorTypeList[y][x] == enemyType -> movePiece(
                x,
                y,
                binding.chessView.positionList[y][x]
            )
        }
    }

    private fun clickEventToOwnPiece(x: Int, y: Int) {
        binding.chessView.clearColorList()

        //clicking same piece
        if (game.targetPosition == Pair(y, x)) {
            game.inTarget = false
            game.targetPosition = null
            return
        }

        //castle with clicking rook or king
        if (castleFromPieceClick(x, y)) {
            game.inTarget = false
            game.targetPosition = null
            return
        }

        game.inTarget = true
        game.targetPosition = Pair(y, x)
        binding.chessView.changeColorType(ChessView.selectedType, x, y)
        var possibleMoves =
            getPossibleMoves(game.whiteTurn, binding.chessView.positionList, Pair(y, x), binding.chessView.positionList[y][x])
        possibleMoves = filterPossibleMoves(possibleMoves)
        showPossibleMoves(possibleMoves)
    }

    private fun filterPossibleMoves(possibleMoves: ArrayList<PossibleMove>, targetPosition: Pair<Int, Int>? = null): ArrayList<PossibleMove> {
        val targetPos = targetPosition ?: game.targetPosition
        val trashMoves = arrayListOf<PossibleMove>()
        for (possibleMove in possibleMoves) {
            val targetPieceInPosition =
                binding.chessView.positionList[possibleMove.y][possibleMove.x]
            var killPiece =
                if (targetPieceInPosition.status == PieceInPosition.isBlank) null else targetPieceInPosition
            val type = getTypeOfMove(possibleMove.x, possibleMove.y, killPiece == null, targetPosition)
            val enemyPieceYCoordinate =
                if (game.whiteTurn) possibleMove.y + 1 else possibleMove.y - 1

            if (type == enPassant)
                killPiece = binding.chessView.positionList[enemyPieceYCoordinate][possibleMove.x]
            val promotionType = if (type == promotion) queen.id else null

            val move = Move(
                "",
                targetPos!!,
                possibleMove.y to possibleMove.x,
                binding.chessView.positionList[targetPos.first][targetPos.second].piece!!,
                game.whiteTurn,
                killPiece,
                type,
                promotionType
            )
            if (!moveIsLegal(move)) {
                trashMoves.add(possibleMove)
            }
        }
        trashMoves.forEach {
            possibleMoves.remove(it)
        }
        return possibleMoves
    }

    private fun moveIsLegal(move: Move): Boolean {
        val positionList = getPositionListByMoves(ArrayList(moves + move))
        val whiteTurn = !game.whiteTurn
        return !isInCheck(ArrayList(positionList.map { ArrayList(it) }), whiteTurn)
    }

    private fun isInCheck(positionList: ArrayList<ArrayList<PieceInPosition>>, whiteTurn: Boolean): Boolean {
        val allPieces: ArrayList<Triple<Int, Int, PieceInPosition>> = arrayListOf()
        positionList.forEachIndexed { y, pieceInPositions ->
            pieceInPositions.forEachIndexed { x, pieceInPosition ->
                if (pieceInPosition.status == whiteTurn.toInt())
                    allPieces.add(Triple(y, x, pieceInPosition))
            }
        }
        for (piece in allPieces) {
            val possibleMoves =
                getPossibleMoves(whiteTurn, positionList, piece.first to piece.second, piece.third)
            if (possibleMoves.any { it.type == kill && positionList[it.y][it.x].piece == king.id && positionList[it.y][it.x].status != whiteTurn.toInt() }) {
                return true
            }
        }
        return false
    }

    private fun getPossibleMoves(
        isWhiteTurn: Boolean,
        positionList: ArrayList<ArrayList<PieceInPosition>>,
        position: Pair<Int, Int>,
        pieceInPosition: PieceInPosition
    ): ArrayList<PossibleMove> {
        val ability = Ability.getAbilityByPieceId(isWhiteTurn, pieceInPosition.piece!!)

        //exceptions for pawn
        tryCatchAndLog {
            exceptionsForPawn(isWhiteTurn, ability, position, positionList, pieceInPosition)
        }

        //exceptions for king
        exceptionsForKing(isWhiteTurn, ability, position, positionList, pieceInPosition)

        //handling limited movements
        val limitedMoves =
            limitedMovements(isWhiteTurn, ability, position, positionList, pieceInPosition)

        //handling unlimited movements
        val unlimitedMoves = unlimitedMovements(isWhiteTurn, ability, position, positionList)
        return ArrayList(limitedMoves + unlimitedMoves)
    }

    private fun showPossibleMoves(possibleMoves: ArrayList<PossibleMove>) {
        for (possibleMove in possibleMoves) {
            when (possibleMove.type) {
                move -> {
                    binding.chessView.changePositionData(
                        PieceInPosition(PieceInPosition.isTarget),
                        possibleMove.x,
                        possibleMove.y
                    )
                }
                kill -> {
                    binding.chessView.changeColorType(enemyType, possibleMove.x, possibleMove.y)
                }
            }
        }
    }

    private fun movePiece(x: Int, y: Int, killPiece: PieceInPosition? = null) {
        val id = UUID.randomUUID().toString()
        val type = getTypeOfMove(x, y, killPiece == null)
        if (type == promotion) {
            getPromotionPiece(y, x, killPiece, id)
        } else {
            val piece =
                binding.chessView.positionList[game.targetPosition!!.first][game.targetPosition!!.second].piece!!
            movePieceWithPromotion(
                Move(
                    id,
                    game.targetPosition!!,
                    Pair(y, x),
                    piece,
                    game.whiteTurn,
                    killPiece,
                    type
                )
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun movePieceWithPromotion(move: Move) {
        val enemyPieceYCoordinate = if (game.whiteTurn) move.to.first + 1 else move.to.first - 1

        if (move.type == enPassant)
            move.killPiece = binding.chessView.positionList[enemyPieceYCoordinate][move.to.second]

        moves.add(move)

        if (game.whiteTurn) {
            doubleMoves.add(move to null)
        } else {
            val lastMove = doubleMoves.last().first
            doubleMoves.removeLast()
            doubleMoves.add(lastMove to move)
        }

        moveAdapter.notifyDataSetChanged()

        val currentPiece = binding.chessView.positionList[move.from.first][move.from.second]

        game.apply {
            if (currentPiece.piece == king.id)
                kingMoved =
                    (whiteTurn || kingMoved.first) to if (whiteTurn) kingMoved.second else true
            else if (currentPiece.piece == rook.id)
                if (move.to.second == 0)
                    queenSideRookMoved =
                        (whiteTurn || kingMoved.first) to if (whiteTurn) kingMoved.second else true
                else if (move.to.second == 7)
                    kingSideRookMoved =
                        (whiteTurn || kingMoved.first) to if (whiteTurn) kingMoved.second else true
        }

        if (move.type == enPassant)
            binding.chessView.changePositionData(
                PieceInPosition(PieceInPosition.isBlank),
                move.to.second,
                enemyPieceYCoordinate
            )
        binding.chessView.changePositionData(currentPiece, move.to.second, move.to.first)
        binding.chessView.changePositionData(
            PieceInPosition(PieceInPosition.isBlank),
            move.from.second,
            move.from.first
        )
        castleIfNeeded(move.to.first, move.type)
        promoteIfNeeded(move)

        binding.chessView.clearColorList()
        game.inTarget = false
        game.targetPosition = null
        game.whiteTurn = !game.whiteTurn

        val isInCheck = isInCheck(binding.chessView.positionList, !game.whiteTurn)

        if (game.whiteTurn)
            game.isInCheck = isInCheck to game.isInCheck.second
        else
            game.isInCheck = game.isInCheck.first to isInCheck

        controlEndings(isInCheck)
    }

    private fun controlEndings(isInCheck: Boolean) {
        val allPieces: ArrayList<Triple<Int, Int, PieceInPosition>> = arrayListOf()
        binding.chessView.positionList.forEachIndexed { y, pieceInPositions ->
            pieceInPositions.forEachIndexed { x, pieceInPosition ->
                if (pieceInPosition.status == game.whiteTurn.toInt())
                    allPieces.add(Triple(y, x, pieceInPosition))
            }
        }
        val allPossibleMoves = arrayListOf<PossibleMove>()
        for (piece in allPieces) {
            var possibleMoves =
                getPossibleMoves(game.whiteTurn, binding.chessView.positionList, piece.first to piece.second, piece.third)
            possibleMoves = filterPossibleMoves(possibleMoves, piece.first to piece.second)
            allPossibleMoves.addAll(possibleMoves)
        }
        var winner: String? = null
        var defeatType: String? = null
        if (isInCheck && allPossibleMoves.isEmpty()) {
            winner = (if (game.whiteTurn) "Black" else "White") + " Wins"
            defeatType = "Checkmate"
        } else if (allPossibleMoves.isEmpty()) {
            winner = "Draw"
            defeatType = "Stalemate"
        } else if (repetitionEnding()) {
            winner = "Draw"
            defeatType = "Repetition"
        } else if (fiftyMoveEnding()) {
            winner = "Draw"
            defeatType = "50 Move Ending"
        } else if (insufficientMaterialEnding()) {
            winner = "Draw"
            defeatType = "Insufficient Material"
        }
        winner?.let {
            showAlertDialog(
                winner,
                defeatType!!,
                positiveButtonText = "Play Again",
                negativeButtonText = "Ok",
                this::playAgain
            )
        }
    }

    private fun insufficientMaterialEnding(): Boolean {
        //TODO: run out of time and has all pieces is draw
        val allPositions = arrayListOf<Triple<Int, Int, PieceInPosition>>()
        binding.chessView.positionList.forEachIndexed { y, pieceInPositions ->
            pieceInPositions.forEachIndexed { x, pieceInPosition ->
                if (pieceInPosition.piece != null)
                    allPositions.add(Triple(y, x, pieceInPosition))
            }
        }
        var twoBishopSameSquareColor = false
        if (allPositions.size == 4 && allPositions.filter { it.third.piece!! != king.id }.all { it.third.piece == bishop.id }) {
            val bishops = allPositions.filter { it.third.piece!! != king.id }
            val notSameColor = bishops[0].third.status != bishops[1].third.status
            val sameSquareColor = (bishops[0].first + bishops[0].second % 2) == (bishops[1].first + bishops[1].second % 2)
            twoBishopSameSquareColor = notSameColor && sameSquareColor
        }

        return (allPositions.size == 2) || //kings look at each other
                (allPositions.size == 3 && (allPositions.any { it.third.piece!! == knight.id } || allPositions.any { it.third.piece!! == bishop.id })) || //one minor piece with king
                (allPositions.size == 4 && (allPositions.filter { it.third.piece!! != king.id }.all { it.third.piece!! == knight.id })) || //two knights without checkmate
                (twoBishopSameSquareColor) //two bishops same color
    }

    private fun fiftyMoveEnding(): Boolean {
        if (moves.size >= 100) {
            val last50moves = moves.slice(moves.size - 100 until moves.size)
            return last50moves.all { it.killPiece == null } && last50moves.all { it.piece != pawn.id }
        }
        return false
    }

    private fun repetitionEnding(): Boolean {
        if (moves.size >= 9) {
            val ctrlPositionList = ArrayList(binding.chessView.positionList.map { ArrayList(it.toMutableList()) }.toMutableList().flatten())
            val lastMoveList = ArrayList(getPositionListByMoves(ArrayList(moves.slice(0 until moves.size - 4))).flatten())
            val firstMoveList = ArrayList(getPositionListByMoves(ArrayList(moves.slice(0 until moves.size - 8))).flatten())
            return compareArrayLists(ctrlPositionList, lastMoveList) && compareArrayLists(ctrlPositionList, firstMoveList)
        }
        return false
    }

    private fun getPositionListByMoves(moves: ArrayList<Move>) : ArrayList<ArrayList<PieceInPosition>> {
        val positionList = PositionList.map { it.toMutableList() }.toMutableList()
        var whiteTurn = true
        for (move in moves) {
            val enemyPieceYCoordinate = if (whiteTurn) move.to.first + 1 else move.to.first - 1
            val currentPiece = positionList[move.from.first][move.from.second]
            if (move.type == enPassant)
                positionList[enemyPieceYCoordinate][move.to.second] =
                    PieceInPosition(PieceInPosition.isBlank)
            positionList[move.to.first][move.to.second] = currentPiece
            positionList[move.from.first][move.from.second] = PieceInPosition(PieceInPosition.isBlank)

            if (move.type in arrayOf(queenSideCastling, kingSideCastling)) {
                val yPos = if (whiteTurn) 7 else 0
                val firstX = if (move.type == queenSideCastling) 0 else 7
                val secondX = if (move.type == queenSideCastling) 3 else 5
                positionList[yPos][secondX] = currentPiece
                positionList[yPos][firstX] = PieceInPosition(PieceInPosition.isBlank)
            }
            val promotePiece =
                PieceInPosition(if (whiteTurn) isWhite else isBlack, move.promotionPiece)
            if (move.type == promotion) {
                positionList[move.to.first][move.to.second] = promotePiece
            }
            whiteTurn = !whiteTurn
        }
        return ArrayList(positionList.map { ArrayList(it) })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun playAgain() {
        game = Game()
        binding.chessView.positionList = ArrayList(PositionList.map { ArrayList(it.toMutableList()) }.toMutableList())
        moves.clear()
        doubleMoves.clear()
        moveAdapter.notifyDataSetChanged()
    }

    private fun castleFromPieceClick(x: Int, y: Int): Boolean {
        if (!game.inTarget)
            return false

        val clickPiece = binding.chessView.positionList[y][x].piece!!
        val targetPiece =
            binding.chessView.positionList[game.targetPosition!!.first][game.targetPosition!!.second].piece!!

        if ((clickPiece == king.id && targetPiece == rook.id) ||
            (clickPiece == rook.id && targetPiece == king.id)
        ) {
            val targetIsKing = targetPiece == king.id
            val yCoordinate = if (game.whiteTurn) 7 else 0
            val queenSide =
                if (targetIsKing) game.targetPosition!!.second - x > 0 else x - game.targetPosition!!.second > 0
            if (game.castleCheck1(
                    queenSide,
                    game.whiteTurn
                ) && binding.chessView.positionList[yCoordinate].filterIndexed { i, _ -> if (queenSide) i in 1..3 else i in 5..6 }
                    .all { it.status == PieceInPosition.isBlank }
            ) {
                game.targetPosition = Pair(yCoordinate, 4)
                val move = Move("", game.targetPosition!!, yCoordinate to if (queenSide) 2 else 6, binding.chessView.positionList[yCoordinate][4].piece!!, game.whiteTurn, null, if (queenSide) queenSideCastling else kingSideCastling)
                if (moveIsLegal(move)) {
                    movePiece(
                        if (queenSide) 2 else 6,
                        yCoordinate,
                    )
                    return true
                }
            }
        }
        return false
    }

    private fun promoteIfNeeded(move: Move) {
        val currentPiece =
            PieceInPosition(if (game.whiteTurn) isWhite else isBlack, move.promotionPiece)
        if (move.type == promotion) {
            binding.chessView.changePositionData(currentPiece, move.to.second, move.to.first)
        }
    }

    private fun getPromotionPiece(y: Int, x: Int, killPiece: PieceInPosition? = null, id: String) {
        lastPromoteData = Triple(Pair(y, x), killPiece, id)
        if (game.whiteTurn) {
            binding.whitePromotion.visibility = View.VISIBLE
        } else {
            binding.blackPromotion.visibility = View.VISIBLE
        }
    }

    private fun castleIfNeeded(y: Int, type: Int) {
        if (type in arrayOf(queenSideCastling, kingSideCastling)) {
            val yPos = if (game.whiteTurn) 7 else 0
            val firstX = if (type == queenSideCastling) 0 else 7
            val secondX = if (type == queenSideCastling) 3 else 5
            val currentPiece = binding.chessView.positionList[y][firstX]
            binding.chessView.changePositionData(currentPiece, secondX, yPos)
            binding.chessView.changePositionData(
                PieceInPosition(PieceInPosition.isBlank),
                firstX,
                yPos
            )
        }
    }

    private fun getTypeOfMove(x: Int, y: Int, killPieceIsNull: Boolean, targetPosition: Pair<Int, Int>? = null): Int {
        val targetPos = targetPosition ?: game.targetPosition!!
        val currentPiece =
            binding.chessView.positionList[targetPos.first][targetPos.second].piece!!
        return if (currentPiece == king.id)
            if (x - targetPos.second == 2)
                kingSideCastling
            else if (x - targetPos.second == -2)
                queenSideCastling
            else normal
        else if (currentPiece == pawn.id)
            if ((game.whiteTurn && y == 0) || (!game.whiteTurn && y == 7))
                promotion
            else if (killPieceIsNull && targetPos.second != x)
                enPassant
            else normal
        else normal
    }

    private fun exceptionsForPawn(
        isWhiteTurn: Boolean,
        ability: Ability,
        position: Pair<Int, Int>,
        positionList: ArrayList<ArrayList<PieceInPosition>>,
        pieceInPosition: PieceInPosition
    ) {
        val destination = if (isWhiteTurn) -1 else 1

        val notBlank =
            positionList[position.first + destination][position.second].status != PieceInPosition.isBlank

        if (pieceInPosition.piece == pawn.id) {
            val newLimitedPairs: ArrayList<Triple<Int, Int, Boolean>> = arrayListOf()
            if ((isWhiteTurn && (position.first != 6 || notBlank)) ||
                (!isWhiteTurn && position.first != 1 || notBlank)
            ) {
                for (pair in ability.limitedPairs!!) {
                    newLimitedPairs.add(
                        Triple(
                            pair.first,
                            pair.second,
                            pair.first != 2 * destination
                        )
                    )
                }
                ability.limitedPairs = newLimitedPairs.toTypedArray()
                newLimitedPairs.clear()
            }
        }
    }

    private fun exceptionsForKing(
        isWhiteTurn: Boolean,
        ability: Ability,
        position: Pair<Int, Int>,
        positionList: ArrayList<java.util.ArrayList<PieceInPosition>>,
        pieceInPosition: PieceInPosition
    ) {
        arrayOf(true, false).forEach { queenSide ->
            if (pieceInPosition.piece == king.id &&
                game.castleCheck1(queenSide, isWhiteTurn)
            ) {
                val newLimitedPairs: ArrayList<Triple<Int, Int, Boolean>> = arrayListOf()
                for (pair in ability.limitedPairs!!) {
                    val castleMove = (pair.first == 0 && pair.second == if (queenSide) -2 else 2)
                    newLimitedPairs.add(
                        Triple(
                            pair.first,
                            pair.second,
                            if (castleMove) positionList[position.first].filterIndexed { i, _ -> if (queenSide) i in 1..3 else i in 5..6 }
                                .all { it.status == PieceInPosition.isBlank } else pair.third
                        )
                    )
                }
                ability.limitedPairs = newLimitedPairs.toTypedArray()
                newLimitedPairs.clear()
            }
        }
    }

    private fun limitedMovements(
        isWhiteTurn: Boolean,
        ability: Ability,
        position: Pair<Int, Int>,
        positionList: ArrayList<ArrayList<PieceInPosition>>,
        pieceInPosition: PieceInPosition
    ): ArrayList<PossibleMove> {
        val possibleMoves = arrayListOf<PossibleMove>()
        ability.limitedPairs?.forEach { pair ->
            if (pair.third) {
                tryCatchAndLog {
                    val targetRow = position.first + pair.first
                    val targetColumn = position.second + pair.second

                    if (targetRow !in 0..7 || targetColumn !in 0..7) {
                        return@tryCatchAndLog
                    }

                    val targetPiece = positionList[targetRow][targetColumn]

                    val isBlankPiece = targetPiece.status == PieceInPosition.isBlank
                    val isPawnPiece = pieceInPosition.piece == pawn.id
                    val isEnemyPiece =
                        if (isWhiteTurn) targetPiece.status == isBlack else targetPiece.status == isWhite

                    val possibleMove = PossibleMove(-1, targetColumn, targetRow)

                    if (isBlankPiece && ((isPawnPiece && pair.second == 0) || !isPawnPiece)) {
                        possibleMove.type = move
                    } else if (isEnemyPiece && !(isPawnPiece && pair.second == 0)) {
                        possibleMove.type = kill
                    } else if (isBlankPiece && moves.isNotEmpty()) {
                        val enemyPieceYCoordinate =
                            if (isWhiteTurn) targetRow + 1 else targetRow - 1
                        val enemyPiecePosition = positionList[enemyPieceYCoordinate][targetColumn]

                        //conditions for en passant
                        val isTrueRank =
                            if (isWhiteTurn) position.first == 3 else position.first == 4
                        val hasEnemyPiece =
                            (if (isWhiteTurn) enemyPiecePosition.status == isBlack else enemyPiecePosition.status == isWhite) &&
                                    enemyPiecePosition.piece == pawn.id
                        val movedBeforeThisMove =
                            (moves.last().to == enemyPieceYCoordinate to targetColumn) &&
                                    (moves.last().to.first - moves.last().from.first).absoluteValue == 2

                        if (isTrueRank && hasEnemyPiece && movedBeforeThisMove) {
                            possibleMove.type = move
                        }
                    }
                    if (possibleMove.type != -1)
                        possibleMoves.add(possibleMove)
                }
            }
        }
        return possibleMoves
    }

    private fun unlimitedMovements(
        whiteTurn: Boolean,
        ability: Ability,
        position: Pair<Int, Int>,
        positionList: ArrayList<ArrayList<PieceInPosition>>
    ): ArrayList<PossibleMove> {
        val possibleMoves = arrayListOf<PossibleMove>()
        fun checkLoop(i: Int, j: Int): Boolean {
            val possibleMove = loopIsFinished(whiteTurn, j, i, positionList[i][j])
            if (possibleMove.third)
                possibleMoves.add(possibleMove.first)
            return possibleMove.second
        }

        if (ability.horizontalMoving == true) {
            for (i in (position.second + 1) until positionList[position.first].size) {
                if (checkLoop(position.first, i)) break
            }
            for (i in (position.second - 1) downTo 0) {
                if (checkLoop(position.first, i)) break
            }
        }

        if (ability.verticalMoving == true) {
            for (i in (position.first + 1) until positionList.map { it[position.second] }.size) {
                if (checkLoop(i, position.second)) break
            }
            for (i in (position.first - 1) downTo 0) {
                if (checkLoop(i, position.second)) break
            }
        }

        if (ability.crossMoving == true) {
            val firstAreaSize = minOf(position.first, position.second)
            val secondAreaSize = minOf(position.first, 7 - position.second)
            val thirdAreaSize = minOf(7 - position.first, position.second)
            val forthAreaSize = minOf(7 - position.first, 7 - position.second)

            for (i in 1..firstAreaSize) {
                if (checkLoop(position.first - i, position.second - i)) break
            }
            for (i in 1..secondAreaSize) {
                if (checkLoop(position.first - i, position.second + i)) break
            }
            for (i in 1..thirdAreaSize) {
                if (checkLoop(position.first + i, position.second - i)) break
            }
            for (i in 1..forthAreaSize) {
                if (checkLoop(position.first + i, position.second + i)) break
            }
        }
        return possibleMoves
    }

    private fun loopIsFinished(
        whiteTurn: Boolean,
        x: Int,
        y: Int,
        pos: PieceInPosition
    ): Triple<PossibleMove, Boolean, Boolean> {
        val possibleMove = PossibleMove(-1, x, y)
        return if (pos.status == PieceInPosition.isBlank) {
            possibleMove.type = move
            Triple(possibleMove, false, true)
        } else if (whiteTurn.toInt() != pos.status) {
            possibleMove.type = kill
            Triple(possibleMove, true, true)
        } else {
            Triple(possibleMove, true, false)
        }
    }

    override fun promote(move: Move) = movePieceWithPromotion(move)

    override fun onClick(v: View) {
        val promotionType: Int? = when (v.id) {
            R.id.b_bishop, R.id.w_bishop -> bishop.id
            R.id.b_queen, R.id.w_queen -> queen.id
            R.id.b_rook, R.id.w_rook -> rook.id
            R.id.b_knight, R.id.w_knight -> knight.id
            else -> null
        }
        promotionType?.let {
            this.promote(
                Move(
                    lastPromoteData!!.third,
                    game.targetPosition!!,
                    lastPromoteData!!.first.first to lastPromoteData!!.first.second,
                    pawn.id,
                    game.whiteTurn,
                    lastPromoteData!!.second,
                    promotion,
                    it
                )
            )
            binding.whitePromotion.visibility = View.GONE
            binding.blackPromotion.visibility = View.GONE
        }
    }

}