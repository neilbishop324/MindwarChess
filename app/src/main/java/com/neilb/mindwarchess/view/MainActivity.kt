package com.neilb.mindwarchess.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.neilb.mindwarchess.R
import com.neilb.mindwarchess.adapter.KillPieceAdapter
import com.neilb.mindwarchess.adapter.MoveAdapter
import com.neilb.mindwarchess.databinding.ActivityMainBinding
import com.neilb.mindwarchess.viewmodel.GameViewModel
import com.neilb.mindwarchess.viewmodel.GameViewModel.Companion.calculatePoint
import com.neilb.mindwarchess.viewmodel.GameViewModel.Companion.getKillPieces
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
import com.neilb.mindwarchess.util.compareArrayLists
import com.neilb.mindwarchess.util.showAlertDialog
import com.neilb.mindwarchess.util.toInt
import com.neilb.mindwarchess.util.tryCatchAndLog
import kotlinx.coroutines.*
import java.util.UUID
import kotlin.math.absoluteValue
import androidx.activity.viewModels
import com.neilb.mindwarchess.util.asBoolean

class MainActivity : AppCompatActivity(), PromotionCallback, OnClickListener {

    private lateinit var binding: ActivityMainBinding

    private val gameViewModel: GameViewModel by viewModels()

    private lateinit var moves: ArrayList<Move>
    private lateinit var doubleMoves: ArrayList<Pair<Move, Move?>>
    private lateinit var moveAdapter: MoveAdapter
    private var lastPromoteData: Triple<Pair<Int, Int>, PieceInPosition?, String>? = null

    private lateinit var blackKillPieceAdapter: KillPieceAdapter
    private lateinit var whiteKillPieceAdapter: KillPieceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {

        moves = arrayListOf()
        doubleMoves = arrayListOf()
        moveAdapter = MoveAdapter(doubleMoves)
        binding.movesList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.movesList.adapter = moveAdapter

        blackKillPieceAdapter = KillPieceAdapter()
        binding.blackKillPieces.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.blackKillPieces.adapter = blackKillPieceAdapter

        whiteKillPieceAdapter = KillPieceAdapter()
        binding.whiteKillPieces.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.whiteKillPieces.adapter = whiteKillPieceAdapter

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
            (gameViewModel.whiteTurn.asBoolean() && positionStatus == isWhite)
                    || (!gameViewModel.whiteTurn.asBoolean() && positionStatus == isBlack) -> clickEventToOwnPiece(x, y)
            gameViewModel.inTarget.asBoolean() && (positionStatus == PieceInPosition.isBlank) -> {
                //losing focus on targeting
                binding.chessView.clearColorList()
                gameViewModel.setInTarget(false)
                gameViewModel.setTargetPosition(null)
            }
            gameViewModel.inTarget.asBoolean() && positionStatus == PieceInPosition.isTarget -> movePiece(x, y)
            gameViewModel.inTarget.asBoolean() && colorTypeList[y][x] == enemyType -> movePiece(
                x,
                y,
                binding.chessView.positionList[y][x]
            )
        }
    }

    private fun clickEventToOwnPiece(x: Int, y: Int) {
        binding.chessView.clearColorList()

        val clickedSamePiece = gameViewModel.targetPosition.value == y to x

        if (clickedSamePiece ||
            //castle with clicking rook or king
            castleFromPieceClick(x, y)) {
            gameViewModel.setInTarget(false)
            gameViewModel.setTargetPosition(null)

            if (clickedSamePiece) {
                return
            }
        }

        gameViewModel.setInTarget(true)
        gameViewModel.setTargetPosition(Pair(y, x))
        binding.chessView.changeColorType(ChessView.selectedType, x, y)

        var possibleMoves =
            getPossibleMoves(gameViewModel.whiteTurn.asBoolean(), binding.chessView.positionList, Pair(y, x), binding.chessView.positionList[y][x])
        possibleMoves = filterPossibleMoves(possibleMoves)
        showPossibleMoves(possibleMoves)
    }

    private fun filterPossibleMoves(possibleMoves: ArrayList<PossibleMove>, targetPosition: Pair<Int, Int>? = null): ArrayList<PossibleMove> {
        val targetPos = targetPosition ?: gameViewModel.targetPosition.value
        val trashMoves = arrayListOf<PossibleMove>()
        for (possibleMove in possibleMoves) {
            val targetPieceInPosition =
                binding.chessView.positionList[possibleMove.y][possibleMove.x]
            var killPiece =
                if (targetPieceInPosition.status == PieceInPosition.isBlank) null else targetPieceInPosition
            val type = getTypeOfMove(possibleMove.x, possibleMove.y, killPiece == null, targetPosition)
            val enemyPieceYCoordinate =
                if (gameViewModel.whiteTurn.asBoolean()) possibleMove.y + 1 else possibleMove.y - 1

            if (type == enPassant)
                killPiece = binding.chessView.positionList[enemyPieceYCoordinate][possibleMove.x]
            val promotionType = if (type == promotion) queen.id else null

            val move = Move(
                "",
                targetPos!!,
                possibleMove.y to possibleMove.x,
                binding.chessView.positionList[targetPos.first][targetPos.second].piece!!,
                gameViewModel.whiteTurn.asBoolean(),
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
        val whiteTurn = !gameViewModel.whiteTurn.asBoolean()
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
                binding.chessView.positionList[gameViewModel.targetPosition.value!!.first][gameViewModel.targetPosition.value!!.second].piece!!
            movePieceWithPromotion(
                Move(
                    id,
                    gameViewModel.targetPosition.value!!,
                    Pair(y, x),
                    piece,
                    gameViewModel.whiteTurn.asBoolean(),
                    killPiece,
                    type
                )
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun movePieceWithPromotion(move: Move) {
        val enemyPieceYCoordinate = if (gameViewModel.whiteTurn.asBoolean()) move.to.first + 1 else move.to.first - 1

        if (move.type == enPassant)
            move.killPiece = binding.chessView.positionList[enemyPieceYCoordinate][move.to.second]

        moves.add(move)

        if (gameViewModel.whiteTurn.asBoolean()) {
            doubleMoves.add(move to null)
        } else {
            val lastMove = doubleMoves.last().first
            doubleMoves.removeLast()
            doubleMoves.add(lastMove to move)
        }

        moveAdapter.notifyDataSetChanged()

        val currentPiece = binding.chessView.positionList[move.from.first][move.from.second]

        gameViewModel.apply {
            if (currentPiece.piece == king.id)
                setKingMoved((whiteTurn.asBoolean() || kingMoved.value!!.first) to if (whiteTurn.asBoolean()) kingMoved.value!!.second else true)
            else if (currentPiece.piece == rook.id)
                if (move.to.second == 0)
                    setQueenSideRookMoved((whiteTurn.asBoolean() || kingMoved.value!!.first) to if (whiteTurn.asBoolean()) kingMoved.value!!.second else true)
                else if (move.to.second == 7)
                    setKingSideRookMoved((whiteTurn.asBoolean() || kingMoved.value!!.first) to if (whiteTurn.asBoolean()) kingMoved.value!!.second else true)
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
        gameViewModel.setInTarget(false)
        gameViewModel.setTargetPosition(null)
        gameViewModel.setWhiteTurn(!gameViewModel.whiteTurn.asBoolean())

        val isInCheck = isInCheck(binding.chessView.positionList, !gameViewModel.whiteTurn.asBoolean())

        if (gameViewModel.whiteTurn.asBoolean())
            gameViewModel.setIsInCheck(isInCheck to gameViewModel.isInCheck.value!!.second)
        else
            gameViewModel.setIsInCheck(gameViewModel.isInCheck.value!!.first to isInCheck)

        changePointsAndChecks()

        controlEndings(isInCheck)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun changePointsAndChecks() {

        //change points

        val whiteKillPoint = calculatePoint(binding.chessView.positionList.flatten(), isWhite = true)
        val blackKillPoint = calculatePoint(binding.chessView.positionList.flatten(), isWhite = false)

        val whitePoint = whiteKillPoint - blackKillPoint
        val blackPoint = blackKillPoint - whiteKillPoint

        if (whitePoint > 0) {
            binding.whitePoint.text = "+$whitePoint"
            binding.whitePoint.visibility = View.VISIBLE
        } else {
            binding.whitePoint.visibility = View.GONE
        }

        if (blackPoint > 0) {
            binding.blackPoint.text = "+$blackPoint"
            binding.blackPoint.visibility = View.VISIBLE
        } else {
            binding.blackPoint.visibility = View.GONE
        }

        blackKillPieceAdapter.pieces = getKillPieces(moves, true).map { isWhite to it }
        whiteKillPieceAdapter.pieces = getKillPieces(moves, false).map { isBlack to it }

        blackKillPieceAdapter.notifyDataSetChanged()
        whiteKillPieceAdapter.notifyDataSetChanged()

    }

    private fun controlEndings(isInCheck: Boolean) {
        val allPieces: ArrayList<Triple<Int, Int, PieceInPosition>> = arrayListOf()
        binding.chessView.positionList.forEachIndexed { y, pieceInPositions ->
            pieceInPositions.forEachIndexed { x, pieceInPosition ->
                if (pieceInPosition.status == gameViewModel.whiteTurn.asBoolean().toInt())
                    allPieces.add(Triple(y, x, pieceInPosition))
            }
        }
        val allPossibleMoves = arrayListOf<PossibleMove>()
        for (piece in allPieces) {
            var possibleMoves =
                getPossibleMoves(gameViewModel.whiteTurn.asBoolean(), binding.chessView.positionList, piece.first to piece.second, piece.third)
            possibleMoves = filterPossibleMoves(possibleMoves, piece.first to piece.second)
            allPossibleMoves.addAll(possibleMoves)
        }
        var winner: String? = null
        var defeatType: String? = null
        if (isInCheck && allPossibleMoves.isEmpty()) {
            winner = (if (gameViewModel.whiteTurn.asBoolean()) "Black" else "White") + " Wins"
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
                positionList[yPos][secondX] = positionList[yPos][firstX]
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
        gameViewModel.playAgain()
        binding.chessView.positionList = ArrayList(PositionList.map { ArrayList(it.toMutableList()) }.toMutableList())
        moves.clear()
        doubleMoves.clear()
        moveAdapter.notifyDataSetChanged()
        blackKillPieceAdapter.pieces = listOf()
        whiteKillPieceAdapter.pieces = listOf()
        blackKillPieceAdapter.notifyDataSetChanged()
        whiteKillPieceAdapter.notifyDataSetChanged()
        binding.whitePoint.text = ""
        binding.whitePoint.visibility = View.GONE
        binding.blackPoint.text = ""
        binding.blackPoint.visibility = View.GONE
    }

    private fun castleFromPieceClick(x: Int, y: Int): Boolean {
        if (!gameViewModel.inTarget.asBoolean())
            return false

        val clickPiece = binding.chessView.positionList[y][x].piece!!
        val targetPiece =
            binding.chessView.positionList[gameViewModel.targetPosition.value!!.first][gameViewModel.targetPosition.value!!.second].piece!!

        if ((clickPiece == king.id && targetPiece == rook.id) ||
            (clickPiece == rook.id && targetPiece == king.id)
        ) {
            val targetIsKing = targetPiece == king.id
            val yCoordinate = if (gameViewModel.whiteTurn.asBoolean()) 7 else 0
            val queenSide =
                if (targetIsKing) gameViewModel.targetPosition.value!!.second - x > 0 else x - gameViewModel.targetPosition.value!!.second > 0
            if (gameViewModel.castleCheck1(
                    queenSide,
                    gameViewModel.whiteTurn.asBoolean()
                ) && binding.chessView.positionList[yCoordinate].filterIndexed { i, _ -> if (queenSide) i in 1..3 else i in 5..6 }
                    .all { it.status == PieceInPosition.isBlank }
            ) {
                gameViewModel.setTargetPosition(Pair(yCoordinate, 4))
                val move = Move("", gameViewModel.targetPosition.value!!, yCoordinate to if (queenSide) 2 else 6, binding.chessView.positionList[yCoordinate][4].piece!!, gameViewModel.whiteTurn.asBoolean(), null, if (queenSide) queenSideCastling else kingSideCastling)
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
            PieceInPosition(if (gameViewModel.whiteTurn.asBoolean()) isWhite else isBlack, move.promotionPiece)
        if (move.type == promotion) {
            binding.chessView.changePositionData(currentPiece, move.to.second, move.to.first)
        }
    }

    private fun getPromotionPiece(y: Int, x: Int, killPiece: PieceInPosition? = null, id: String) {
        lastPromoteData = Triple(Pair(y, x), killPiece, id)
        if (gameViewModel.whiteTurn.asBoolean()) {
            binding.whitePromotion.visibility = View.VISIBLE
        } else {
            binding.blackPromotion.visibility = View.VISIBLE
        }
    }

    private fun castleIfNeeded(y: Int, type: Int) {
        if (type in arrayOf(queenSideCastling, kingSideCastling)) {
            val yPos = if (gameViewModel.whiteTurn.asBoolean()) 7 else 0
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
        val targetPos = targetPosition ?: gameViewModel.targetPosition.value!!
        val currentPiece =
            binding.chessView.positionList[targetPos.first][targetPos.second].piece!!
        return if (currentPiece == king.id)
            if (x - targetPos.second == 2)
                kingSideCastling
            else if (x - targetPos.second == -2)
                queenSideCastling
            else normal
        else if (currentPiece == pawn.id)
            if ((gameViewModel.whiteTurn.asBoolean() && y == 0) || (!gameViewModel.whiteTurn.asBoolean() && y == 7))
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
                gameViewModel.castleCheck1(queenSide, isWhiteTurn)
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
                    gameViewModel.targetPosition.value!!,
                    lastPromoteData!!.first.first to lastPromoteData!!.first.second,
                    pawn.id,
                    gameViewModel.whiteTurn.asBoolean(),
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