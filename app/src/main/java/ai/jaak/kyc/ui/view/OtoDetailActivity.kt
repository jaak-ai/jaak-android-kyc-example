package ai.jaak.kyc.ui.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ai.jaak.kyc.R
import ai.jaak.kyc.data.model.ComparisonItem
import ai.jaak.kyc.data.model.ImageQualityItem
import ai.jaak.kyc.data.model.PropertyResultItem
import ai.jaak.kyc.databinding.ActivityOtoDetailBinding
import ai.jaak.kyc.ui.adapter.ComparisonAdapter
import ai.jaak.kyc.ui.adapter.ImageQualityAdapter
import ai.jaak.kyc.ui.adapter.PropertyResultAdapter

class OtoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtoDetailBinding

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupImageClickListeners()
        loadOtoData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageClickListeners() {
        // Click en imagen 1 para preview full screen
        binding.ivImage1.setOnClickListener {
            showImagePreview(binding.ivImage1)
        }

        // Click en imagen 2 para preview full screen
        binding.ivImage2.setOnClickListener {
            showImagePreview(binding.ivImage2)
        }
    }

    /**
     * Muestra preview de imagen en pantalla completa
     */
    private fun showImagePreview(imageView: ImageView) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_preview)

        val previewImage = dialog.findViewById<ImageView>(R.id.ivPreview)
        val closeButton = dialog.findViewById<ImageView>(R.id.ivClose)

        previewImage.setImageDrawable(imageView.drawable)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadOtoData() {
        // Obtener datos del Intent
        val otoData = intent.getParcelableExtra<ai.jaak.kyc.data.model.OtoDetailData>("oto_data")

        if (otoData == null) {
            android.util.Log.e("OtoDetailActivity", "❌ No se recibieron datos de OTO")
            android.widget.Toast.makeText(this, "Error: Datos no disponibles", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("OtoDetailActivity", "========== OTO DATA RECEIVED ==========")
        android.util.Log.d("OtoDetailActivity", "Event ID: ${otoData.eventId}")
        android.util.Log.d("OtoDetailActivity", "Image 1 URL: ${otoData.image1Url}")
        android.util.Log.d("OtoDetailActivity", "Image 2 URL: ${otoData.image2Url}")
        android.util.Log.d("OtoDetailActivity", "Comparison Score: ${otoData.comparisonScore}")
        android.util.Log.d("OtoDetailActivity", "Distance: ${otoData.distance}")
        android.util.Log.d("OtoDetailActivity", "Processing Time: ${otoData.processingTime}")
        android.util.Log.d("OtoDetailActivity", "========== JSON COMPLETO ==========")
        android.util.Log.d("OtoDetailActivity", "Accessories Data JSON: ${otoData.accessoriesData}")
        android.util.Log.d("OtoDetailActivity", "Quality Data JSON: ${otoData.qualityData}")
        android.util.Log.d("OtoDetailActivity", "======================================")

        // ========== CARGAR IMÁGENES ==========
        if (!otoData.image1Url.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(otoData.image1Url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivImage1)
        } else {
            binding.ivImage1.setImageResource(R.drawable.ic_person)
        }

        if (!otoData.image2Url.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(otoData.image2Url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivImage2)
        } else {
            binding.ivImage2.setImageResource(R.drawable.ic_person)
        }

        // ========== ACCESORIOS FACIALES ==========
        val accessoriesTime = otoData.accessoriesProcessingTime ?: "N/A"
        binding.tvProcessingTimeAccessories.text = "Tiempo de procesamiento: $accessoriesTime"
        setupAccessoriesRecyclerViews(otoData.accessoriesData)

        // ========== COMPARACIÓN 1:1 ==========
        if (otoData.comparisonScore != null) {
            val comparisonData = listOf(
                ComparisonItem(
                    property = "Score",
                    dataReceived = String.format("%.2f%%", otoData.comparisonScore),
                    dataExpected = "Mayor a 96%",
                    result = if (otoData.comparisonScore >= 96.0) "Correcto" else "Incorrecto",
                    isCorrect = otoData.comparisonScore >= 96.0
                ),
                ComparisonItem(
                    property = "Distance",
                    dataReceived = String.format("%.4f", otoData.distance ?: 0.0),
                    dataExpected = "Mayor a 0.4",
                    result = if ((otoData.distance ?: 0.0) > 0.4) "Correcto" else "Incorrecto",
                    isCorrect = (otoData.distance ?: 0.0) > 0.4
                )
            )

            binding.rvComparison.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = ComparisonAdapter(comparisonData, showHeader = true)
            }
        }

        val comparisonTime = otoData.processingTime ?: "N/A"
        binding.tvProcessingTimeComparison.text = "Tiempo de procesamiento: $comparisonTime"

        // ========== CALIDAD DE IMAGEN ==========
        val qualityTime = otoData.qualityProcessingTime ?: "N/A"
        binding.tvProcessingTimeQuality1.text = "Tiempo de procesamiento: $qualityTime"
        binding.tvProcessingTimeQuality2.text = "Tiempo de procesamiento: $qualityTime"
        setupQualityRecyclerViews(otoData.qualityData)

        // Ocultar loading
        binding.progressBar.visibility = View.GONE
    }

    private fun setupAccessoriesRecyclerViews(accessoriesJson: String?) {
        if (accessoriesJson.isNullOrEmpty()) {
            // Sin datos disponibles
            val noData = listOf(PropertyResultItem("Sin datos", "N/A", false))
            binding.rvAccessoriesImage1.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = PropertyResultAdapter(noData)
            }
            binding.rvAccessoriesImage2.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = PropertyResultAdapter(noData)
            }
            return
        }

        try {
            val gson = com.google.gson.Gson()
            val accessoriesMap = gson.fromJson(accessoriesJson, Map::class.java) as Map<*, *>

            // Parsear imagen 1
            val image1Data = accessoriesMap["image1"] as? Map<*, *>
            val accessoriesImage1 = parseAccessories(image1Data)

            // Parsear imagen 2
            val image2Data = accessoriesMap["image2"] as? Map<*, *>
            val accessoriesImage2 = parseAccessories(image2Data)

            binding.rvAccessoriesImage1.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = PropertyResultAdapter(accessoriesImage1)
            }

            binding.rvAccessoriesImage2.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = PropertyResultAdapter(accessoriesImage2)
            }
        } catch (e: Exception) {
            android.util.Log.e("OtoDetailActivity", "Error parseando accesorios: ${e.message}", e)
            val errorData = listOf(PropertyResultItem("Error", "No se pudieron cargar los datos", false))
            binding.rvAccessoriesImage1.adapter = PropertyResultAdapter(errorData)
            binding.rvAccessoriesImage2.adapter = PropertyResultAdapter(errorData)
        }
    }

    private fun parseAccessories(data: Map<*, *>?): List<PropertyResultItem> {
        if (data == null) return listOf(PropertyResultItem("Sin datos", "N/A", false))

        val items = mutableListOf<PropertyResultItem>()

        // glass
        val glass = data["glass"] as? Boolean ?: false
        items.add(PropertyResultItem(
            "Lentes",
            if (glass) "Detectado" else "No detectado",
            !glass // Correcto si NO tiene lentes
        ))

        // hat
        val hat = data["hat"] as? Boolean ?: false
        items.add(PropertyResultItem(
            "Gorro",
            if (hat) "Detectado" else "No detectado",
            !hat // Correcto si NO tiene gorro
        ))

        // mask
        val mask = data["mask"] as? Boolean ?: false
        items.add(PropertyResultItem(
            "Mascarilla",
            if (mask) "Detectado" else "No detectado",
            !mask // Correcto si NO tiene mascarilla
        ))

        // sunGlass
        val sunGlass = data["sunGlass"] as? Boolean ?: false
        items.add(PropertyResultItem(
            "Lentes oscuros",
            if (sunGlass) "Detectado" else "No detectado",
            !sunGlass // Correcto si NO tiene lentes oscuros
        ))

        return items
    }

    private fun setupQualityRecyclerViews(qualityJson: String?) {
        if (qualityJson.isNullOrEmpty()) {
            val noData = listOf(ImageQualityItem("Sin datos", "N/A", "N/A", false))
            binding.rvQualityImage1.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = ImageQualityAdapter(noData)
            }
            binding.rvQualityImage2.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = ImageQualityAdapter(noData)
            }
            return
        }

        try {
            val gson = com.google.gson.Gson()
            val qualityMap = gson.fromJson(qualityJson, Map::class.java) as Map<*, *>

            // Parsear imagen 1
            val image1Data = qualityMap["image1"] as? Map<*, *>
            val qualityImage1 = parseQuality(image1Data)

            // Parsear imagen 2
            val image2Data = qualityMap["image2"] as? Map<*, *>
            val qualityImage2 = parseQuality(image2Data)

            binding.rvQualityImage1.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = ImageQualityAdapter(qualityImage1)
            }

            binding.rvQualityImage2.apply {
                layoutManager = LinearLayoutManager(this@OtoDetailActivity)
                adapter = ImageQualityAdapter(qualityImage2)
            }
        } catch (e: Exception) {
            android.util.Log.e("OtoDetailActivity", "Error parseando calidad: ${e.message}", e)
            val errorData = listOf(ImageQualityItem("Error", "No se pudieron cargar los datos", "N/A", false))
            binding.rvQualityImage1.adapter = ImageQualityAdapter(errorData)
            binding.rvQualityImage2.adapter = ImageQualityAdapter(errorData)
        }
    }

    private fun parseQuality(data: Map<*, *>?): List<ImageQualityItem> {
        if (data == null) return listOf(ImageQualityItem("Sin datos", "N/A", "N/A", false))

        val items = mutableListOf<ImageQualityItem>()

        // 1. Borrosidad (blur) - Debe ser menor a 0.71
        val blur = (data["blur"] as? Number)?.toDouble() ?: 0.0
        val blurStr = String.format("%.2f", blur)
        items.add(ImageQualityItem(
            property = "Borrosidad",
            imageData = blurStr,
            expectedData = "Menor a 0.71",
            isCorrect = blur < 0.71
        ))

        // 2. Brillo (brightness) - Clasificación según web: 2=normal, 3+=alto
        // Correcto si es "Brillo normal", Incorrecto si es "Brillo alto"
        val brightness = (data["brightness"] as? Number)?.toDouble() ?: 0.0
        val brightnessLabel = when {
            brightness < 2.0 -> "Brillo bajo"
            brightness < 3.0 -> "Brillo normal"
            else -> "Brillo alto"
        }
        items.add(ImageQualityItem(
            property = "Brillo",
            imageData = brightnessLabel,
            expectedData = "Brillo normal",
            isCorrect = brightness >= 2.0 && brightness < 3.0 // Correcto solo si es "Brillo normal"
        ))

        // 3. Altura (height) - Debe ser mayor o igual a 400 px
        val height = (data["height"] as? Number)?.toInt() ?: 0
        items.add(ImageQualityItem(
            property = "Altura",
            imageData = "$height px",
            expectedData = "Mayor a 400 px",
            isCorrect = height >= 400
        ))

        // 4. Ancho (width) - Debe ser mayor o igual a 400 px
        val width = (data["width"] as? Number)?.toInt() ?: 0
        items.add(ImageQualityItem(
            property = "Ancho",
            imageData = "$width px",
            expectedData = "Mayor a 400 px",
            isCorrect = width >= 400
        ))

        // 5. Rotación horizontal - Debe estar entre -10 y 10 grados
        // IMPORTANTE: El servicio envía valores con offset de +50, hay que restar 50
        val hRotationRaw = (data["horizontal_rotation"] as? Number)?.toDouble() ?: 0.0
        val hRotation = hRotationRaw - 50.0
        items.add(ImageQualityItem(
            property = "Rotación horizontal",
            imageData = String.format("%.2f grados", hRotation),
            expectedData = "Entre -10 grados y 10 grados",
            isCorrect = hRotation >= -10.0 && hRotation <= 10.0
        ))

        // 6. Rotación vertical - Debe estar entre -5 y 5 grados
        // IMPORTANTE: El servicio envía valores con offset de +50, hay que restar 50
        val vRotationRaw = (data["vertical_rotation"] as? Number)?.toDouble() ?: 0.0
        val vRotation = vRotationRaw - 50.0
        items.add(ImageQualityItem(
            property = "Rotación vertical",
            imageData = String.format("%.2f grados", vRotation),
            expectedData = "Entre -5 grados y 5 grados",
            isCorrect = vRotation >= -5.0 && vRotation <= 5.0
        ))

        // 7. Rostros en la imagen - Debe ser exactamente 1
        val numFaces = (data["number_faces"] as? Number)?.toInt() ?: 0
        items.add(ImageQualityItem(
            property = "Rostros en la imagen",
            imageData = numFaces.toString(),
            expectedData = "1",
            isCorrect = numFaces == 1
        ))

        // 8. Tamaño de rostro en la imagen - Debe ser mayor o igual a 25%
        val sizeFace = (data["size_face"] as? Number)?.toDouble() ?: 0.0
        items.add(ImageQualityItem(
            property = "Tamaño de rostro en la imagen",
            imageData = String.format("%.0f %%", sizeFace),
            expectedData = "Mayor a 25 %",
            isCorrect = sizeFace >= 25.0
        ))

        return items
    }
}
