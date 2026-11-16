package soka;

import java.util.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

public class CloudEnv {
    public static final int DATACENTERS = 1;
    public static final int HOSTS_PER_DATACENTER = 2;
    public static final int VMS_PER_HOST = 2;
    public static final double[] VM_MIPS = createVmMips();

    public static void validateTopology() {
        validateTopology(DATACENTERS, HOSTS_PER_DATACENTER, VMS_PER_HOST, VM_MIPS.length);
    }

    private static void validateTopology(int datacenters, int hostsPerDatacenter,
                                         int vmsPerHost, int totalVms) {
        if (datacenters != 1 || hostsPerDatacenter < 2 || vmsPerHost < 2) {
            throw new IllegalStateException(
                    "Topologi wajib: 1 datacenter, minimal 2 host, minimal 2 VM per host");
        }
        if (totalVms != datacenters * hostsPerDatacenter * vmsPerHost) {
            throw new IllegalStateException("Jumlah VM tidak sesuai topologi");
        }
    }

    private static double[] createVmMips() {
        double[] mips = new double[DATACENTERS * HOSTS_PER_DATACENTER * VMS_PER_HOST];
        for (int i = 0; i < mips.length; i++) {
            // Each host contains one small and one large VM.
            mips[i] = i % VMS_PER_HOST < VMS_PER_HOST / 2 ? 500 : 1000;
        }
        return mips;
    }

    public static List<Cloudlet> simulate(int[] mapping, List<Long> miList) throws Exception {
        validateTopology();
        CloudSim.init(1, Calendar.getInstance(), false);

        for (int d = 0; d < DATACENTERS; d++) {
            List<Host> hosts = new ArrayList<>();
            for (int h = 0; h < HOSTS_PER_DATACENTER; h++) {
                List<Pe> pes = new ArrayList<>();
                for (int pe = 0; pe < VMS_PER_HOST; pe++) {
                    pes.add(new Pe(pe, new PeProvisionerSimple(1000)));
                }
                hosts.add(new Host(d * HOSTS_PER_DATACENTER + h,
                        new RamProvisionerSimple(16384), new BwProvisionerSimple(10000),
                        1_000_000, pes, new VmSchedulerTimeShared(pes)));
            }
            DatacenterCharacteristics dc = new DatacenterCharacteristics(
                    "x86", "Linux", "Xen", hosts, 10.0, 3.0, 0.05, 0.001, 0.0);
            final int datacenterIndex = d;
            VmAllocationPolicySimple policy = new VmAllocationPolicySimple(hosts) {
                @Override
                public org.cloudbus.cloudsim.core.HostEntity findHostForGuest(
                        org.cloudbus.cloudsim.core.GuestEntity guest) {
                    int globalHost = guest.getId() / VMS_PER_HOST;
                    if (globalHost / HOSTS_PER_DATACENTER != datacenterIndex) return null;
                    Host host = hosts.get(globalHost % HOSTS_PER_DATACENTER);
                    return host.isSuitableForGuest(guest) ? host : null;
                }
            };
            new Datacenter("DC_" + d, dc, policy, new LinkedList<Storage>(), 0);
        }

        DatacenterBroker broker = new DatacenterBroker("Broker");

        // VM
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < VM_MIPS.length; i++) {
            boolean small = VM_MIPS[i] == 500;
            vms.add(new Vm(i, broker.getId(), VM_MIPS[i], 1, small ? 512 : 1024,
                    1000, 10000, "Xen", new CloudletSchedulerSpaceShared()));
        }
        broker.submitGuestList(vms);

        // Cloudlet
        List<Cloudlet> cloudlets = new ArrayList<>();
        UtilizationModel full = new UtilizationModelFull();
        for (int i = 0; i < miList.size(); i++) {
            Cloudlet c = new Cloudlet(i, miList.get(i), 1, 300, 300, full, full, full);
            c.setUserId(broker.getId());
            cloudlets.add(c);
        }
        broker.submitCloudletList(cloudlets);

        for (int i = 0; i < mapping.length; i++)
            broker.bindCloudletToVm(cloudlets.get(i).getCloudletId(), mapping[i]);

        CloudSim.startSimulation();
        List<Cloudlet> done = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();
        return done;
    }
}
