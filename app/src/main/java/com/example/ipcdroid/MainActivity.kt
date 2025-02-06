class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var button: Button
    private var camera: Camera? = null
    private var rtspServer: RtspServerCamera2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        button = findViewById(R.id.button)

        // 初始化 RTSP 服务器
        rtspServer = RtspServerCamera2(this, true, object : ConnectCheckerRtsp {
            override fun onConnectionSuccessRtsp() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connection success", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onConnectionFailedRtsp(reason: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connection failed: $reason", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNewBitrateRtsp(bitrate: Long) {}
            override fun onDisconnectRtsp() {}
        })

        // 设置端口
        rtspServer?.setPort(1935)

        button.setOnClickListener {
            if (rtspServer?.isStreaming == true) {
                rtspServer?.stopStream()
                button.text = "Start Stream"
            } else {
                startCamera()
                button.text = "Stop Stream"
            }
        }

        requestPermissions()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                camera = rtspServer?.let { cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, it.audioRecord, it
                ) }

            } catch(exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtspServer?.stopStream()
    }
}
