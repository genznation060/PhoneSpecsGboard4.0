package com.example.data.tsv

import com.example.data.model.SpecCategory

data class CanonicalColumn(
    val index: Int,
    val name: String,
    val category: SpecCategory
)

object CanonicalSpecs {
    val COLUMNS = listOf(
        CanonicalColumn(0, "Brand", SpecCategory.OTHER),
        CanonicalColumn(1, "Model", SpecCategory.OTHER),
        CanonicalColumn(2, "Full Name", SpecCategory.OTHER),
        CanonicalColumn(3, "Release Date", SpecCategory.OTHER),
        CanonicalColumn(4, "Price", SpecCategory.OTHER),
        CanonicalColumn(5, "Dimensions", SpecCategory.DESIGN),
        CanonicalColumn(6, "Weight", SpecCategory.DESIGN),
        CanonicalColumn(7, "Build Material", SpecCategory.DESIGN),
        CanonicalColumn(8, "IP Rating", SpecCategory.DESIGN),
        CanonicalColumn(9, "SIM Type", SpecCategory.DESIGN),
        CanonicalColumn(10, "Display Type", SpecCategory.DISPLAY),
        CanonicalColumn(11, "Display Size", SpecCategory.DISPLAY),
        CanonicalColumn(12, "Resolution", SpecCategory.DISPLAY),
        CanonicalColumn(13, "Refresh Rate", SpecCategory.DISPLAY),
        CanonicalColumn(14, "Peak Brightness", SpecCategory.DISPLAY),
        CanonicalColumn(15, "Screen Protection", SpecCategory.DISPLAY),
        CanonicalColumn(16, "Operating System", SpecCategory.SOFTWARE),
        CanonicalColumn(17, "Chipset", SpecCategory.PERFORMANCE),
        CanonicalColumn(18, "CPU", SpecCategory.PERFORMANCE),
        CanonicalColumn(19, "GPU", SpecCategory.PERFORMANCE),
        CanonicalColumn(20, "RAM", SpecCategory.PERFORMANCE),
        CanonicalColumn(21, "Storage", SpecCategory.PERFORMANCE),
        CanonicalColumn(22, "SD Card Slot", SpecCategory.PERFORMANCE),
        CanonicalColumn(23, "Main Camera", SpecCategory.CAMERA),
        CanonicalColumn(24, "Ultra-Wide Camera", SpecCategory.CAMERA),
        CanonicalColumn(25, "Telephoto Camera", SpecCategory.CAMERA),
        CanonicalColumn(26, "Video Recording", SpecCategory.CAMERA),
        CanonicalColumn(27, "Front Camera", SpecCategory.CAMERA),
        CanonicalColumn(28, "Battery Capacity", SpecCategory.BATTERY),
        CanonicalColumn(29, "Wired Charging", SpecCategory.BATTERY),
        CanonicalColumn(30, "Wireless Charging", SpecCategory.BATTERY),
        CanonicalColumn(31, "Reverse Wireless", SpecCategory.BATTERY),
        CanonicalColumn(32, "5G Bands", SpecCategory.CONNECTIVITY),
        CanonicalColumn(33, "Wi-Fi Standard", SpecCategory.CONNECTIVITY),
        CanonicalColumn(34, "Bluetooth", SpecCategory.CONNECTIVITY),
        CanonicalColumn(35, "NFC", SpecCategory.CONNECTIVITY),
        CanonicalColumn(36, "Biometrics", SpecCategory.SECURITY),
        CanonicalColumn(37, "Speakers & Audio", SpecCategory.OTHER),
        CanonicalColumn(38, "Colors", SpecCategory.DESIGN)
    )

    fun getColumn(index: Int): CanonicalColumn {
        return COLUMNS.getOrNull(index) ?: CanonicalColumn(
            index = index,
            name = "Extra Column $index",
            category = SpecCategory.OTHER
        )
    }

    val KNOWN_BRANDS = listOf(
        "Samsung", "Apple", "Vivo", "Oppo", "Xiaomi", "OnePlus", "Google", "Realme",
        "Motorola", "Sony", "Asus", "Huawei", "Honor", "Nothing", "Infinix", "Tecno",
        "Poco", "iQOO", "ZTE", "Nubia", "Meizu", "Lenovo", "Sharp", "Nokia", "HMD"
    )

    val SAMPLE_SEED_TSV = """
Samsung	Galaxy S26 Ultra	Samsung Galaxy S26 Ultra	2026, February	$1,299	162.8 x 77.6 x 8.2 mm	218 g	Titanium frame, Gorilla Armor 2 back	IP68 dust/water resistant	Dual SIM (Nano-SIM and eSIM)	Dynamic LTPO AMOLED 2X, 120Hz	6.9 inches	1440 x 3120 pixels (~500 ppi)	120 hertz	3000 nits peak	Corning Gorilla Armor 2	Android 16, One UI 8.0	Qualcomm Snapdragon 8 Elite (3 nm)	Octa-core (2x4.32 GHz Oryon)	Adreno 830	16 GB LPDDR5X	512 GB UFS 4.1	No	200 MP, f/1.7, OIS	50 MP, f/1.9, 120˚ ultrawide	50 MP, f/3.4, 5x periscope OIS	8K@30fps, 4K@120fps	12 MP, f/2.2, 4K@60fps	5000 milliamp hours	65 watt wired	25 watt wireless Qi2	4.5 watt reverse wireless	5G SA/NSA/Sub6/mmWave	Wi-Fi 7 (802.11be)	Bluetooth 5.4	Yes	Ultrasonic In-display	Stereo speakers, AKG	Titanium Black, Titanium Gray
Vivo	Vivo X300 Pro	Vivo X300 Pro	2026, January	$1,199	164.1 x 75.3 x 8.4 mm	224 g	Aluminum frame, AG frosted glass	IP68/IP69 dust/water resistant	Dual Nano-SIM	AMOLED, 1B colors, 120Hz, Zeiss	6.78 inches	1260 x 2800 pixels (~453 ppi)	120 hertz	3200 nits peak	Armor Glass Glass	Android 16, Funtouch 16	MediaTek Dimensity 9400+ (3 nm)	Octa-core (1x3.63 GHz Cortex-X925)	Immortalis-G925	16 GB LPDDR5X	512 GB UFS 4.1	No	50 MP, 1-inch Sony LYT-900, OIS	50 MP, f/2.0, 119˚ ultrawide	200 MP Zeiss APO telephoto 3.7x OIS	4K@120fps, 10-bit Log	50 MP, f/2.0, AF	5400 milliamp hours	100 watt wired	50 watt wireless	Reverse wired	5G Dual Standby	Wi-Fi 7	Bluetooth 5.4, aptX Lossless	Yes	Optical In-display	Stereo Hi-Fi speakers	Titanium, Carbon Black
Apple	iPhone 18 Pro Max	Apple iPhone 18 Pro Max	2026, September	$1,199	163.0 x 77.6 x 8.25 mm	227 g	Grade 5 Titanium frame, Ceramic Shield	IP68 water resistant (6m for 30m)	eSIM only (US) / Nano+eSIM	Super Retina XDR OLED, ProMotion	6.9 inches	1320 x 2868 pixels (~460 ppi)	120 hertz	3000 nits peak	Ceramic Shield 2	iOS 20	Apple A20 Pro (2 nm)	6-core CPU (2 performance + 4 efficiency)	6-core Apple GPU	12 GB LPDDR5X	256 GB / 512 GB / 1 TB	No	48 MP, f/1.78, sensor-shift OIS	48 MP, f/2.2, 120˚ ultrawide	48 MP, f/2.8, 5x tetraprism OIS	4K ProRes@120fps, Dolby Vision	12 MP TrueDepth with autofocus	4685 milliamp hours	35 watt wired	25 watt MagSafe Qi2	4.5 watt reverse wired	5G NR Sub-6 and mmWave	Wi-Fi 7	Bluetooth 5.3	Yes (Apple Pay)	Face ID 3D Facial Recognition	Stereo speakers with Spatial Audio	Natural Titanium, Desert Titanium
OnePlus	OnePlus 13	OnePlus 13	2025, November	$899	162.9 x 76.5 x 8.5 mm	213 g	Ceramic / Eco Leather back, aluminum	IP68/IP69 water and dust proof	Dual SIM (Nano-SIM)	LTPO 4.0 AMOLED, Dolby Vision	6.82 inches	1440 x 3168 pixels (~510 ppi)	120 hertz	4500 nits peak	Crystal Shield Glass	OxygenOS 15, Android 15	Snapdragon 8 Elite	Octa-core Oryon	Adreno 830	16 GB / 24 GB	512 GB / 1 TB	No	50 MP Sony LYT-808, OIS, Hasselblad	50 MP, f/2.0, 120˚ ultrawide	50 MP periscope 3x optical zoom	8K@30fps, 4K@60fps Dolby Vision	32 MP, f/2.4	6000 milliamp hours	100 watt SuperVOOC	50 watt AirVOOC	10 watt reverse wireless	5G Full Netcom	Wi-Fi 7	Bluetooth 5.4	Yes	Ultrasonic Fingerprint	Stereo speakers, Dolby Atmos	Midnight Ocean, Arctic Dawn
Google	Pixel 10 Pro XL	Google Pixel 10 Pro XL	2026, August	$1,099	162.8 x 76.6 x 8.5 mm	221 g	Polished aluminum frame, Matte glass	IP68 water and dust resistant	Single Nano-SIM and eSIM	Super Actua LTPO OLED, 120Hz	6.8 inches	1344 x 2992 pixels (~486 ppi)	120 hertz	3000 nits peak	Gorilla Glass Victus 2	Android 16	Google Tensor G5 (TSMC 3nm)	Octa-core custom CPU	Immortalis-G720	16 GB LPDDR5X	256 GB / 512 GB / 1 TB	No	50 MP Octa PD wide, OIS	48 MP Quad PD ultrawide, Macro	48 MP Quad PD 5x telephoto, OIS	4K@60fps, Video Boost, Night Sight	42 MP Dual PD ultrawide, 103˚	5060 milliamp hours	45 watt wired	23 watt wireless Pixel Stand	Reverse wireless share	5G Sub-6 / mmWave	Wi-Fi 7	Bluetooth 5.4	Yes	Ultrasonic In-display & Face Unlock	Stereo speakers, Spatial Audio	Obsidian, Porcelain, Hazel
    """.trimIndent()
}
