package soka;

import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import org.cloudbus.cloudsim.Cloudlet;
import soka.algo.*;

public class Main {
    static Path output = Path.of(".");
    static double mean(double[] a) { return Arrays.stream(a).average().orElseThrow(); }
    static double std(double[] a) {
        double mu = mean(a), sum = 0;
        for (double x : a) sum += (x - mu) * (x - mu);
        return a.length < 2 ? 0 : Math.sqrt(sum / (a.length - 1));
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && !args[0].equals("--full") && !args[0].equals("--range")) {
            Demo.run(Integer.parseInt(args[0]));
            return;
        }
        int first = 100, last = 500;
        if (args.length > 0 && args[0].equals("--range")) {
            if (args.length != 4) throw new IllegalArgumentException("Usage: --range first last output-directory");
            first = Integer.parseInt(args[1]);
            last = Integer.parseInt(args[2]);
            output = Path.of(args[3]);
        }
        if (first < 100 || last > 1000 || first > last || first % 100 != 0 || last % 100 != 0)
            throw new IllegalArgumentException("Rentang harus kelipatan 100 dalam 100-1000.");
        int sizes = (last - first) / 100 + 1;
        Files.createDirectories(output);
        Locale.setDefault(Locale.US);
        CloudEnv.validateTopology();
        org.cloudbus.cloudsim.Log.disable();
        Files.createDirectories(output.resolve("task-distribution"));
        double[] mips = CloudEnv.VM_MIPS;
        double[][] weights = {{0.5, 0.5}, {0.7, 0.3}, {0.3, 0.7}};
        System.out.println("=".repeat(94));
        System.out.println("TASK SCHEDULING METAHEURISTIK - ABC DAN CUCKOO SEARCH");
        System.out.printf("1 datacenter | %d host | %d VM per host | total %d VM%n",
                CloudEnv.HOSTS_PER_DATACENTER, CloudEnv.VMS_PER_HOST, mips.length);
        System.out.printf("%d-%d task | 3 bobot | 3 run | seed 1000-1002 | populasi 30 | iterasi 100%n", first, last);
        System.out.println("Folder hasil: " + output.toAbsolutePath().normalize());
        System.out.println("Task T1 = baris pertama dataset. Host dan VM memakai ID mulai 0.");
        System.out.println("Durasi = MI/MIPS. Waktu mulai/selesai adalah waktu antrean model, relatif awal task.");
        System.out.println("Tabel pembagian terminal: bobot 0.5/0.5, run 3; BUKAN gabungan tiga run.");
        System.out.println("Daftar lengkap semua run: task-assignments.csv. Daftar per VM: task-distribution/.");
        int validations = 0;
        try (PrintWriter runs = new PrintWriter(output.resolve("results.csv").toFile());
             PrintWriter summary = new PrintWriter(output.resolve("summary.csv").toFile());
             PrintWriter assignments = new PrintWriter(output.resolve("task-assignments.csv").toFile())) {
            runs.println("tasks,algo,w1,w2,run,makespan,util,waiting,fitness");
            summary.println("tasks,algo,w1,w2,runs,mk_mean,mk_std,util_mean,util_std,wait_mean,wait_std,fit_mean,fit_std");
            assignments.println("tasks,algo,w1,w2,run,seed,task_id,mi,host_id,vm_id,vm_mips,queue_position,start_s,duration_s,finish_s");
            for (int tasks = first; tasks <= last; tasks += 100) {
                Path dataset = Path.of("GoCJ Google Cloud Jobs Dataset/GoCJ_Dataset_" + tasks + ".txt");
                if (!Files.exists(dataset)) dataset = Path.of("..").resolve(dataset);
                List<Long> lengths = new ArrayList<>();
                for (String line : Files.readAllLines(dataset))
                    if (!line.isBlank()) lengths.add(Long.parseLong(line.trim()));
                if (lengths.size() != tasks) throw new IllegalStateException("Jumlah dataset tidak sesuai: " + dataset);
                double[] mi = lengths.stream().mapToDouble(Long::doubleValue).toArray();
                System.out.println("\n" + "=".repeat(94));
                System.out.printf("UKURAN %d/%d - %d TASK | dataset: %s%n", (tasks - first) / 100 + 1, sizes, tasks, dataset.getFileName());
                StringBuilder comparisons = new StringBuilder();
                for (double[] w : weights) {
                    for (Scheduler algo : List.of(new ABC(w[0], w[1], 30, 100, 20),
                            new CuckooSearch(w[0], w[1], 30, 100, 0.25))) {
                        double[] mk = new double[3], ut = new double[3], wait = new double[3], fit = new double[3];
                        int[] lastMap = null;
                        for (int r = 0; r < 3; r++) {
                            int[] map = algo.schedule(mi, mips, new Random(1000 + r));
                            checkMapping(map, tasks, mips.length);
                            mk[r] = Metrics.makespan(map, mi, mips);
                            ut[r] = Metrics.utilization(map, mi, mips);
                            wait[r] = Metrics.avgWaiting(map, mi, mips);
                            fit[r] = Metrics.fitness(map, mi, mips, w[0], w[1]);
                            runs.printf("%d,%s,%.1f,%.1f,%d,%.3f,%.3f,%.3f,%.5f%n",
                                    tasks, algo.name(), w[0], w[1], r + 1, mk[r], ut[r], wait[r], fit[r]);
                            writeAssignments(assignments, algo.name(), w, r + 1, map, mi, mips);
                            lastMap = map;
                        }
                        summary.printf("%d,%s,%.1f,%.1f,3,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.5f,%.5f%n",
                                tasks, algo.name(), w[0], w[1], mean(mk), std(mk), mean(ut), std(ut),
                                mean(wait), std(wait), mean(fit), std(fit));
                        comparisons.append(String.format("%-5s %.1f/%.1f %21s %19s %21s%n", algo.name(), w[0], w[1],
                                String.format("%.3f +/- %.3f", mean(mk), std(mk)),
                                String.format("%.3f +/- %.3f", mean(ut), std(ut)),
                                String.format("%.3f +/- %.3f", mean(wait), std(wait))));
                        if (w == weights[0]) {
                            distribution(algo.name(), lastMap, mi, mips);
                            validate(algo.name(), lastMap, lengths, mi, mips);
                            validations++;
                        }
                    }
                }
                System.out.println("\nB. PERBANDINGAN METRIK - rata-rata +/- standar deviasi dari 3 run");
                System.out.printf("%-5s %-7s %21s %19s %21s%n", "Algo", "Bobot", "Makespan (s)", "Utilisasi (%)", "Waiting (s)");
                System.out.print(comparisons);
            }
        }
        System.out.printf("%nSELESAI: %d run | %d ringkasan | %d validasi CloudSim LULUS%n", sizes * 18, sizes * 6, validations);
        System.out.println("Hasil: results.csv | summary.csv | task-assignments.csv | task-distribution/");
    }

    static void checkMapping(int[] map, int tasks, int vms) {
        if (map.length != tasks) throw new IllegalStateException("Jumlah mapping salah");
        for (int vm : map) if (vm < 0 || vm >= vms) throw new IllegalStateException("Indeks VM salah");
    }

    static void writeAssignments(PrintWriter out, String algo, double[] w, int run,
                                 int[] map, double[] mi, double[] mips) {
        double[] clocks = new double[mips.length];
        int[] positions = new int[mips.length];
        for (int i = 0; i < map.length; i++) {
            int vm = map[i];
            double duration = mi[i] / mips[vm];
            out.printf("%d,%s,%.1f,%.1f,%d,%d,%d,%.0f,%d,%d,%.0f,%d,%.3f,%.3f,%.3f%n",
                    map.length, algo, w[0], w[1], run, 999 + run, i + 1, mi[i],
                    vm / CloudEnv.VMS_PER_HOST, vm, mips[vm], ++positions[vm], clocks[vm], duration, clocks[vm] + duration);
            clocks[vm] += duration;
        }
    }

    static void distribution(String algo, int[] map, double[] mi, double[] mips) throws Exception {
        String file = "task-distribution/" + map.length + "-" + algo + "-balanced-run3.txt";
        System.out.printf("%nA. PEMBAGIAN TASK %s | %d task | bobot 0.5/0.5 | run 3 | seed 1002%n", algo, map.length);
        System.out.printf("%-6s %-5s %6s %7s %14s  %s%n", "Host", "VM", "MIPS", "Task", "Beban (s)", "Contoh ID task (maks. 6)");
        double[] busy = Metrics.vmBusyTime(map, mi, mips);
        int total = 0;
        try (PrintWriter detail = new PrintWriter(output.resolve(file).toFile())) {
            detail.printf("%s | %d task | bobot 0.5/0.5 | run 3 | seed 1002%n", algo, map.length);
            detail.println("T1 = baris pertama dataset. Urutan ID di setiap VM = urutan eksekusi antrean.");
            for (int vm = 0; vm < mips.length; vm++) {
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < map.length; i++) if (map[i] == vm) ids.add("T" + (i + 1));
                int host = vm / CloudEnv.VMS_PER_HOST;
                total += ids.size();
                String sample = String.join(", ", ids.subList(0, Math.min(6, ids.size())));
                if (ids.size() > 6) sample += ", ...";
                if (ids.isEmpty()) sample = "(kosong)";
                System.out.printf("H%-5d V%-4d %6.0f %7d %14.3f  %s%n", host, vm, mips[vm], ids.size(), busy[vm], sample);
                detail.printf("%nHost H%d -> VM V%d | %.0f MIPS | %d task | beban %.3f s%n", host, vm, mips[vm], ids.size(), busy[vm]);
                for (int offset = 0; offset < ids.size(); offset += 12)
                    detail.println("  " + String.join(", ", ids.subList(offset, Math.min(offset + 12, ids.size()))));
            }
        }
        System.out.printf("Total pembagian: %d/%d task | seluruh task mendapat tepat satu VM%n", total, map.length);
        System.out.println("Daftar ID lengkap: " + file);
    }

    static void validate(String algo, int[] map, List<Long> lengths, double[] mi, double[] mips) throws Exception {
        List<Cloudlet> done = CloudEnv.simulate(map, lengths);
        double start = done.stream().mapToDouble(Cloudlet::getExecStartTime).min().orElseThrow();
        double finish = done.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElseThrow();
        double expected = Metrics.makespan(map, mi, mips);
        if (done.size() != mi.length || Math.abs(finish - start - expected) > 0.01)
            throw new IllegalStateException("Validasi CloudSim gagal: " + algo);
        System.out.printf("Validasi CloudSim: LULUS | %d/%d selesai | makespan %.3f s (analitis %.3f s)%n",
                done.size(), mi.length, finish - start, expected);
    }
}
