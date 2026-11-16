# Rekap metaheuristik 100-1000 task

Rekap ini menggabungkan hasil 100, 200, 300, 400 task dari root repository dengan hasil 500, 600, 700, 800, 900, 1000 task dari `../500-1000/`. Setiap ukuran hanya dicatat sekali. Ini penggabungan hasil dua eksekusi, bukan satu eksekusi baru.

Konfigurasi: khusus ABC dan Cuckoo Search; 1 datacenter, 4 host, 8 VM; populasi 30, 100 iterasi; tiga bobot dan tiga seed 1000-1002.

- `results.csv`: 180 run.
- `summary.csv`: 60 ringkasan.
- `task-assignments.csv`: 99.000 penempatan task.
- `task-distribution/`: 20 laporan pemetaan bobot seimbang, run ketiga.

Bukti validasi berasal dari log root untuk 100-400 dan log `../500-1000/simulation-output.txt` untuk 500-1000, total 20 validasi yang dipilih tanpa duplikasi ukuran. Arsip eksekusi penuh sebelumnya tetap berada di `../../archive/` lokal.

Angka 450 run pada dokumentasi lama mencakup lima algoritma. Rekap ini hanya dua metaheuristik sehingga jumlahnya 180 run.
