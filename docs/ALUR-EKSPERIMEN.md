# Alur eksperimen host heterogen 100-1000 task

Profil aktif: 1 datacenter, 2 Host A (1 PE x 1000 MIPS, RAM 2048 MB), 2 Host B (1 PE x 2000 MIPS, RAM 4096 MB). H0/H1 masing-masing dua VM 500 MIPS; H2/H3 masing-masing dua VM 1000 MIPS. Total 6000 MIPS.

```mermaid
flowchart TD
    A([Mulai]) --> B["Siapkan infrastruktur<br/>2 Host A + 2 Host B, total 8 VM"]
    B --> C["Baca GoCJ sesuai ukuran<br/>100, 200, 300, ..., 1000 task"]
    C --> D["Untuk setiap bobot: 0.5/0.5, 0.7/0.3, 0.3/0.7<br/>Jalankan ABC lalu Cuckoo Search"]
    D --> E["Optimasi masing-masing 3 run<br/>Populasi 30, iterasi 100, seed 1000-1002"]
    E --> F["Petakan setiap task ke VM<br/>Hitung metrik dan simpan penempatan"]
    F --> G["Bobot seimbang, run ketiga:<br/>buat host dan VM di CloudSim"]
    G --> H["Periksa placement 8 VM<br/>Jalankan task dan validasi makespan"]
    H --> I["Tampilkan pembagian per host/VM<br/>dan perbandingan rata-rata tiga run"]
    I --> J["Ulangi seluruh bobot dan ukuran<br/>sampai 1000 task"]
    J --> K["Simpan CSV, laporan per VM,<br/>bukti topology, log dan screenshot"]
    K --> L([Selesai: 180 run, 60 ringkasan, 20 validasi])
    classDef setup fill:#e0f2fe,color:#0c4a6e,stroke:#0284c7;
    classDef search fill:#dcfce7,color:#14532d,stroke:#16a34a;
    classDef verify fill:#fef3c7,color:#78350f,stroke:#d97706;
    classDef result fill:#ede9fe,color:#4c1d95,stroke:#7c3aed;
    class B,C setup;
    class D,E,F search;
    class G,H verify;
    class I,J,K,L result;
```

1. Baca GoCJ sesuai ukuran 100, 200, hingga 1000.
2. Untuk setiap ukuran, uji bobot 0.5/0.5, 0.7/0.3, 0.3/0.7.
3. Pada setiap bobot, jalankan ABC lalu CS, masing-masing tiga run, populasi 30 dan iterasi 100.
4. Catat pemetaan dan metrik setiap run; hitung ringkasan tiga run.
5. Run ketiga pada bobot seimbang divalidasi di CloudSim, termasuk placement VM, jumlah task selesai, dan toleransi makespan 0.01 detik.
6. Simpan hasil di `experiments/infrastruktur-gambar/`, kemudian dokumentasikan sepuluh ukuran secara berurutan.

Hasil eksekusi aktif: 180 run, 60 ringkasan, 20 validasi lolos, 99.000 penempatan task, dan 20 laporan per VM. Hasil ini berasal dari satu eksekusi profil host heterogen. Foto terbaru pada README utama sesuai dengan hasil tersebut.
