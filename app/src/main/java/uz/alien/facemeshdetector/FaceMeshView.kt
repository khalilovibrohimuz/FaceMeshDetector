package uz.alien.facemeshdetector


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.Triangle
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint


class FaceMeshView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var w = 0
    private var h = 0
    private val scale = 1

    private var prevMode = PrevMode.HALF_BLACK
    private var bitmap: Bitmap
    private var points: List<FaceMeshPoint> = emptyList()
    private var triangles: List<Triangle<FaceMeshPoint>> = emptyList()
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply {
        strokeWidth = 0.2f
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.FaceMeshView, 0, 0).apply {
            val resourceId = getResourceId(R.styleable.FaceMeshView_src, 0)
            bitmap = if (resourceId != 0) BitmapFactory.decodeResource(resources, resourceId) else Bitmap.createBitmap(720, 1555, Bitmap.Config.ARGB_8888)
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w
        this.h = h
        setBitmap(bitmap)
    }

    fun setBitmap(bitmap: Bitmap) {
        val scaledHeight = bitmap.height * w / bitmap.width
        val scaledWidth = bitmap.width * h / bitmap.height

        this.bitmap = if (scaledHeight < h) Bitmap.createScaledBitmap(bitmap, w, scaledHeight, true)
        else Bitmap.createScaledBitmap(bitmap, scaledWidth, h, true)

        val options = FaceMeshDetectorOptions.Builder().setUseCase(FaceMeshDetectorOptions.FACE_MESH).build()
        val detector = FaceMeshDetection.getClient(options)

        this.points = emptyList()
        this.triangles = emptyList()

        val scaledBitmap = Bitmap.createScaledBitmap(this.bitmap, this.bitmap.width / scale, this.bitmap.height / scale, true)

        detector.process(InputImage.fromBitmap(scaledBitmap, 0))
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    this.points = face.allPoints
                    this.triangles = face.allTriangles
                }
                invalidate()
            }
            .addOnFailureListener { e -> Log.d("FaceMeshInfo", "Failed $e") }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (prevMode == PrevMode.BITMAP) canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
        if (prevMode == PrevMode.BLACK) canvas.drawColor(Color.BLACK)
        if (prevMode == PrevMode.GRAY) canvas.drawColor(Color.DKGRAY)
        if (prevMode == PrevMode.BLUE) canvas.drawColor(Color.BLUE)
        if (prevMode == PrevMode.HALF_BLACK) canvas.drawColor(Color.argb(240, 0, 0, 0))

        val left = (width - bitmap.width) / 2f
        val top = (height - bitmap.height) / 2f
        for (point in points) canvas.drawCircle(point.position.x * scale + left, point.position.y * scale + top, 3.0f, paint)
        for (triangle in triangles) {
            val points = triangle.allPoints
            if (points.size == 3) {
                val path = Path().apply {
                    moveTo(points[0].position.x * scale + left, points[0].position.y * scale + top)
                    lineTo(points[1].position.x * scale + left, points[1].position.y * scale + top)
                    lineTo(points[2].position.x * scale + left, points[2].position.y * scale + top)
                    close()
                }
                canvas.drawPath(path, linePaint)
            }
        }
    }

    fun setPrevMode(prevMode: PrevMode) {
        this.prevMode = prevMode
    }

    companion object {
        enum class PrevMode {
            BITMAP,
            BLACK,
            GRAY,
            BLUE,
            HALF_BLACK,
            TRANSPARENT
        }
    }
}