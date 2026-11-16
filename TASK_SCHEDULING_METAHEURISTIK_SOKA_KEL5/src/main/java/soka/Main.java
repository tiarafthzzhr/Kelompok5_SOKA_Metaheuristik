package soka;

import java.io.PrintWriter;
import java.util.*;
import org.cloudbus.cloudsim.Cloudlet;
import soka.algo.*;

public class Main {
    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static double std(double[] a) {
        if (a.length < 2) return 0;
        double mu = mean(a), s = 0;
        for (double x : a) s += (x - mu) * (x - mu);
        return Math.sqrt(s / (a.length - 1));
    }

    public static void main(String[] args) throws Exception {
        CloudEnv.validateTopology();
        Locale.setDefault(Locale.US);
        org.cloudbus.cloudsim.Log.enable();
        printTopology();

        double[] vmMips = CloudEnv.VM_MIPS;

        double[][] W = {{0.5, 0.5}, {0.7, 0.3}, {0.3, 0.7}};
        int RUNS = 3;

        try (PrintWriter runs = new PrintWriter("results.csv");
             PrintWriter sum = new PrintWriter("summary.csv")) {
            runs.println("tasks,algo,w1,w2,run,makespan,util,waiting,fitness");
            sum.println("tasks,algo,w1,w2,runs,mk_mean,mk_std,util_mean,util_std,wait_mean,wait_std,fit_mean,fit_std");

            System.out.println("Pengujian 100-1000 task, 3 run per kombinasi");
            for (int tasks = 100; tasks <= 1000; tasks += 100) {
                List<Long> miList = new ArrayList<>();
                for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("../GoCJ Google Cloud Jobs Dataset/GoCJ_Dataset_" + tasks + ".txt"))) {
                    if (!line.isBlank()) miList.add(Long.parseLong(line.trim()));
                }
                if (miList.size() != tasks) throw new IllegalStateException("Jumlah dataset tidak sesuai");
                double[] mi = new double[miList.size()];
                for (int i = 0; i < miList.size(); i++) {
                    mi[i] = Objects.requireNonNull(miList.get(i), "Panjang task tidak boleh null").doubleValue();
                }

            for (double[] w : W) {
                List<Scheduler> algos = List.of(new ABC(w[0], w[1], 30, 100, 20), new CuckooSearch(w[0], w[1], 30, 100, 0.25));

                System.out.printf("%n=== %d TASK | Bobot w1=%.1f, w2=%.1f ===%n", tasks, w[0], w[1]);

                StringBuilder summaryTable = new StringBuilder();
                int algorithmNumber = 0;
                for (Scheduler s : algos) {
                    System.out.printf("%n--- ALGORITMA %d/%d: %s | %d task | w1=%.1f, w2=%.1f ---%n",
                            ++algorithmNumber, algos.size(), s.name(), tasks, w[0], w[1]);
                    System.out.printf("%-10s %3s %14s %14s %14s %10s%n", "Algoritma", "Run", "Makespan (s)", "Util (%)", "Waiting (s)", "Fitness");
                    System.out.println("-".repeat(82));
                    int n = RUNS;
                    double[] mk = new double[n], ut = new double[n], wt = new double[n], fi = new double[n];
                    int[] lastMap = null;
                    for (int r = 0; r < n; r++) {
                        int[] map = s.schedule(mi, vmMips, new Random(1000 + r));
                        mk[r] = Metrics.makespan(map, mi, vmMips);
                        ut[r] = Metrics.utilization(map, mi, vmMips);
                        wt[r] = Metrics.avgWaiting(map, mi, vmMips);
                        fi[r] = Metrics.fitness(map, mi, vmMips, w[0], w[1]);
                        runs.printf("%d,%s,%.1f,%.1f,%d,%.3f,%.3f,%.3f,%.5f%n",
                                tasks, s.name(), w[0], w[1], r + 1, mk[r], ut[r], wt[r], fi[r]);
                        System.out.printf("%-10s %3d %14.3f %14.3f %14.3f %10.5f%n", s.name(), r + 1, mk[r], ut[r], wt[r], fi[r]);
                        lastMap = map;
                    }
                    sum.printf("%d,%s,%.1f,%.1f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.5f,%.5f%n",
                            tasks, s.name(), w[0], w[1], n, mean(mk), std(mk), mean(ut), std(ut),
                            mean(wt), std(wt), mean(fi), std(fi));
                    summaryTable.append(String.format("%-10s %20s %21s %21s %10.5f%n",
                            s.name(), String.format("%.3f +/- %.3f", mean(mk), std(mk)),
                            String.format("%.3f +/- %.3f", mean(ut), std(ut)),
                            String.format("%.3f +/- %.3f", mean(wt), std(wt)), mean(fi)));
                    System.out.printf("Ringkasan %s: makespan %.2f +/- %.2f s | util %.2f +/- %.2f %% | wait %.2f +/- %.2f s | fit %.5f%n",
                            s.name(), mean(mk), std(mk), mean(ut), std(ut), mean(wt), std(wt), mean(fi));
                    if (w == W[0]) validate(s.name(), lastMap, miList, mi, vmMips);
                    else System.out.println("Validasi CloudSim hanya dijalankan pada bobot 0.5/0.5, run ke-3.");
                }
                System.out.println("\nRingkasan 3 pengulangan (rata-rata +/- standar deviasi):");
                System.out.printf("%-10s %20s %21s %21s %10s%n", "Algoritma", "Makespan (s)", "Util (%)", "Waiting (s)", "Fitness");
                System.out.println("-".repeat(105));
                System.out.print(summaryTable);
            }
            }
            System.out.println("Hasil lengkap: results.csv | Ringkasan: summary.csv");
        }
        printFinalTopology();
    }

    static void printFinalTopology() {
        CloudEnv.validateTopology();
        int totalHosts = CloudEnv.DATACENTERS * CloudEnv.HOSTS_PER_DATACENTER;
        System.out.println("\n" + "=".repeat(90));
        System.out.println("SIMULASI SELESAI - RINGKASAN KONFIGURASI TOPOLOGI CLOUDSIM");
        System.out.println("Algoritma yang dijalankan: Artificial Bee Colony (ABC) dan Cuckoo Search (CS) - metaheuristik.");
        System.out.printf("%d datacenter -> %d host -> masing-masing %d VM%n",
                CloudEnv.DATACENTERS, totalHosts, CloudEnv.VMS_PER_HOST);
        for (int d = 0; d < CloudEnv.DATACENTERS; d++) {
            System.out.printf("Datacenter DC_%d%n", d);
            for (int h = 0; h < CloudEnv.HOSTS_PER_DATACENTER; h++) {
                int host = d * CloudEnv.HOSTS_PER_DATACENTER + h;
                StringJoiner vmIds = new StringJoiner(", ");
                for (int v = 0; v < CloudEnv.VMS_PER_HOST; v++) {
                    vmIds.add("VM #" + (host * CloudEnv.VMS_PER_HOST + v));
                }
                System.out.printf("  Host #%d -> %s (jumlah: %d VM)%n",
                        host, vmIds, CloudEnv.VMS_PER_HOST);
            }
        }
        System.out.printf("TOTAL: %d datacenter | %d host | %d VM%n",
                CloudEnv.DATACENTERS, totalHosts, CloudEnv.VM_MIPS.length);
        System.out.println("SYARAT TOPOLOGI TERPENUHI: 1 datacenter, minimal 2 host, minimal 2 VM per host.");
        System.out.println("Cloudlet adalah task yang diproses bergantian pada VM, bukan host atau VM tambahan.");
        System.out.println("Ringkasan ini menunjukkan konfigurasi simulasi CloudSim.");
        System.out.println("=".repeat(90));
    }

    static void printTopology() {
        System.out.println("=".repeat(90));
        System.out.println("SIMULASI PENJADWALAN TASK - CLOUDSIM");
        System.out.printf("%d datacenter | %d host/datacenter | %d VM/host | total %d host dan %d VM%n",
                CloudEnv.DATACENTERS, CloudEnv.HOSTS_PER_DATACENTER, CloudEnv.VMS_PER_HOST,
                CloudEnv.DATACENTERS * CloudEnv.HOSTS_PER_DATACENTER, CloudEnv.VM_MIPS.length);
        for (int d = 0; d < CloudEnv.DATACENTERS; d++) {
            System.out.printf("DC_%d%n", d);
            for (int h = 0; h < CloudEnv.HOSTS_PER_DATACENTER; h++) {
                int host = d * CloudEnv.HOSTS_PER_DATACENTER + h;
                int firstVm = host * CloudEnv.VMS_PER_HOST;
                System.out.printf("  Host #%d -> VM #%d-#%d | %d PE x 1000 MIPS | RAM 16384 MB%n",
                        host, firstVm, firstVm + CloudEnv.VMS_PER_HOST - 1, CloudEnv.VMS_PER_HOST);
            }
        }
        System.out.printf("Setiap host: %d VM x 500 MIPS/512 MB + %d VM x 1000 MIPS/1024 MB%n",
                CloudEnv.VMS_PER_HOST / 2, CloudEnv.VMS_PER_HOST - CloudEnv.VMS_PER_HOST / 2);
        System.out.println("Algoritma yang dijalankan: Artificial Bee Colony (ABC) dan Cuckoo Search (CS) - metaheuristik.");
        System.out.println("Urutan: task 100-1000 -> bobot -> ABC dan CS -> run 1-3");
        System.out.println("Validasi dan log CloudSim: run ke-3 pada bobot 0.5/0.5 setiap algoritma.");
        System.out.println("=".repeat(90));
    }

    static void validate(String name, int[] map, List<Long> miList, double[] mi, double[] vmMips) throws Exception {
        System.out.printf("%n>>> MULAI VALIDASI CLOUDSIM: %s | %d task | run ke-3 <<<%n", name, mi.length);
        List<Cloudlet> done = CloudEnv.simulate(map, miList);
        double minStart = Double.MAX_VALUE, maxFinish = 0;
        for (Cloudlet c : done) {
            minStart = Math.min(minStart, c.getExecStartTime());
            maxFinish = Math.max(maxFinish, c.getExecFinishTime());
        }
        double expected = Metrics.makespan(map, mi, vmMips);
        if (done.size() != mi.length || Math.abs(maxFinish - minStart - expected) > 0.01) {
            throw new IllegalStateException("Validasi CloudSim gagal untuk " + name);
        }
        System.out.printf("   [validasi %s] %d/%d selesai | CloudSim %.2f vs analitis %.2f%n",
                name, done.size(), mi.length, maxFinish - minStart, Metrics.makespan(map, mi, vmMips));
        System.out.printf(">>> SELESAI VALIDASI CLOUDSIM: %s <<<%n%n", name);
    }
}
