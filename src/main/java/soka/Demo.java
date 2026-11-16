package soka;

import java.nio.file.*;
import java.util.*;
import org.cloudbus.cloudsim.Cloudlet;
import soka.algo.*;

/** Demonstrasi kecil, terpisah dari CSV eksperimen lengkap. */
public class Demo {
    public static void run(int tasks) throws Exception {
        if (tasks < 1 || tasks > 20) throw new IllegalArgumentException("Demo menerima 1-20 task.");
        Locale.setDefault(Locale.US);
        CloudEnv.validateTopology();
        org.cloudbus.cloudsim.Log.disable();
        Path dataset = Path.of("GoCJ Google Cloud Jobs Dataset/GoCJ_Dataset_100.txt");
        if (!Files.exists(dataset)) dataset = Path.of("../GoCJ Google Cloud Jobs Dataset/GoCJ_Dataset_100.txt");
        List<Long> lengths = new ArrayList<>();
        for (String line : Files.readAllLines(dataset)) {
            if (!line.isBlank()) lengths.add(Long.parseLong(line.trim()));
            if (lengths.size() == tasks) break;
        }
        if (lengths.size() != tasks) throw new IllegalStateException("Dataset tidak cukup.");
        double[] mi = lengths.stream().mapToDouble(Long::doubleValue).toArray();
        double[] mips = CloudEnv.VM_MIPS;
        System.out.println("============================================================");
        System.out.println("DEMO TASK SCHEDULING METAHEURISTIK");
        System.out.println("============================================================");
        System.out.printf("1. KONFIGURASI%n   %d task | %d datacenter | %d host | %d VM%n",
                tasks, CloudEnv.DATACENTERS, CloudEnv.HOSTS_PER_DATACENTER, mips.length);
        System.out.println("   Data: baris awal GoCJ 100 | bobot 0.5/0.5 | seed 1000");
        System.out.println("   Populasi 30 | iterasi 100 | 1 run per algoritma");
        System.out.println("   Waktu hasil adalah detik simulasi.");
        List<Scheduler> algorithms = List.of(new ABC(0.5, 0.5, 30, 100, 20),
                new CuckooSearch(0.5, 0.5, 30, 100, 0.25));
        double[][] metrics = new double[2][4];
        int index = 0;
        for (Scheduler algorithm : algorithms) {
            System.out.printf("%n%d. HASIL %s%n", index + 2, algorithm.name());
            int[] mapping = algorithm.schedule(mi, mips, new Random(1000));
            System.out.printf("   %-5s %10s %5s %7s %12s%n", "Task", "MI", "Host", "VM", "Durasi (s)");
            for (int i = 0; i < tasks; i++) {
                System.out.printf("   %-5d %10.0f %5d %7d %12.3f%n", i + 1, mi[i],
                        mapping[i] / CloudEnv.VMS_PER_HOST, mapping[i], mi[i] / mips[mapping[i]]);
            }
            double mk = Metrics.makespan(mapping, mi, mips);
            List<Cloudlet> done = CloudEnv.simulate(mapping, lengths);
            double start = done.stream().mapToDouble(Cloudlet::getExecStartTime).min().orElseThrow();
            double finish = done.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElseThrow();
            if (done.size() != tasks || Math.abs(finish - start - mk) > 0.01)
                throw new IllegalStateException("Validasi CloudSim gagal: " + algorithm.name());
            metrics[index++] = new double[]{mk, Metrics.utilization(mapping, mi, mips),
                    Metrics.avgWaiting(mapping, mi, mips), Metrics.fitness(mapping, mi, mips, 0.5, 0.5)};
            System.out.printf("   Validasi CloudSim: LULUS (%d/%d selesai)%n", done.size(), tasks);
        }
        System.out.println("\n4. RINGKASAN PERBANDINGAN");
        System.out.printf("   %-6s %12s %12s %12s %10s%n", "Algo", "Makespan(s)", "Utilisasi(%)", "Waiting(s)", "Fitness");
        for (int i = 0; i < algorithms.size(); i++)
            System.out.printf("   %-6s %12.3f %12.3f %12.3f %10.5f%n", algorithms.get(i).name(),
                    metrics[i][0], metrics[i][1], metrics[i][2], metrics[i][3]);
        System.out.println("\nSelesai. Demo tidak menimpa CSV hasil eksperimen 100-1000 task.");
    }
}
