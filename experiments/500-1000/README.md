# Dokumentasi lanjutan 500-1000 task

ABC dan Cuckoo Search, 1 datacenter, 4 host, 8 VM. Ukuran: 500, 600, 700, 800, 900, 1000. Tiga bobot dan tiga run per algoritma menghasilkan **108 run, 36 ringkasan, 12 validasi CloudSim lolos**. File task-assignments.csv memuat 81.000 penempatan task.

Dari root repository jalankan ulang:

```powershell
.\run-metaheuristik.ps1 -StartTasks 500 -EndTasks 1000 -OutputDirectory experiments/500-1000
```

Untuk menyimpan log, tambahkan `2>&1 | Tee-Object experiments/500-1000/simulation-output.txt` pada perintah tersebut. Folder ini sudah tersedia; menjalankan ulang menimpa isinya yang dihasilkan program.

## Urutan dokumentasi

Buka file output berikut secara berurutan dan ambil screenshot bagian pembagian ABC, pembagian CS, dan tabel perbandingan:

1. [Output 500 task](output-500-task.txt).
2. [Output 600 task](output-600-task.txt).
3. [Output 700 task](output-700-task.txt).
4. [Output 800 task](output-800-task.txt).
5. [Output 900 task](output-900-task.txt).
6. [Output 1000 task](output-1000-task.txt).

Pemetaan terminal adalah bobot 0.5/0.5, run ketiga, seed 1002. Ringkasan metrik merupakan rata-rata dan standar deviasi tiga run. Laporan ID lengkap per VM ada di `task-distribution/`; log lengkap ada di `simulation-output.txt`. Screenshot baru belum dibuat; yang tersedia adalah keluaran simulasi nyata untuk didokumentasikan.

Hasil 500 task dijalankan ulang sebagai penghubung dengan dokumentasi sebelumnya. Jangan menjumlahkannya dua kali saat membuat rekap 100-1000.
