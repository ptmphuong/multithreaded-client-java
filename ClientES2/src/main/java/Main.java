import output.CsvWriter;
import output.RequestInfo;
import output.RequestInfoList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class Main {
    private final static Logger logger =
            Logger.getLogger(Main.class.getName());
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        logger.info("Prompt Input Info");
        // Ask for parameters via command line
//        input.InputInfo info = new input.InputInfo();
//        info.ask();

        // Manually create info

        String lbIp = "skier3LB-2075162823.us-east-1.elb.amazonaws.com";
        String localhost = "localhost";
        InputInfo info = new InputInfo(
                128, 20000, 40, 10, lbIp, "skier3_war"
        );

        int numThreads = info.getNumThreads();
        int numThreadsQuarter = numThreads/4;

        int phase2wait = divideRoundUp(numThreadsQuarter, 10);
        int phase3wait = divideRoundUp(numThreads, 10);

        CountDownLatch cld1 = new CountDownLatch(phase2wait);
        CountDownLatch cld2 = new CountDownLatch(phase3wait);

        logger.info("Start main. Num threads = " + numThreads);
        logger.info("Start phase");

//        CheckPost.send(info);
        LocalDateTime LDTStart = LocalDateTime.now();
        long start = System.currentTimeMillis();

        Phase phase1 = new Phase(1, info, cld1);
        Phase phase2 = new Phase(2, info, cld2);
        Phase phase3 = new Phase(3, info, new CountDownLatch(0));

        phase1.execute();
        cld1.await();
        phase2.execute();
        cld2.await();
        phase3.execute();

        phase1.shutdown();
        phase2.shutdown();
        phase3.shutdown();

        long end = System.currentTimeMillis();
//        CheckPost.send(info);

        logger.info("End phase");
        LocalDateTime LDTEnd = LocalDateTime.now();

        List<Phase> allPhases = new ArrayList<>(Arrays.asList(phase1, phase2, phase3));
        int totalPost = 0;

        System.out.println("Time start: " + LDTStart);
        System.out.println("Time end: " + LDTEnd);

        System.out.println("-------PART 1-------");
        System.out.println(info);
        System.out.println(String.format("Client setting: %d threads share 1 client", phase1.getNUM_THREAD_PER_CLIENT()));

        for (Phase p: allPhases) {
            String postReport = String.format("PhaseID: %d. Total success: %d, Total fail: %d", p.getPhaseID(), p.getTotalSuccessPost(), p.getTotalFailPost());
            System.out.println(postReport);
            totalPost += p.getTotalSuccessPost();
            totalPost += p.getTotalFailPost();
        }

        long runTime = end - start;
        double throughPut = totalPost / ((double) (runTime)/1000);
        String timeReport = String.format("Total post: %d, Total time (millis): %d", totalPost, runTime);
        System.out.println(timeReport);
        System.out.println("throughput (req/sec): " + throughPut);

        // part2
        List<RequestInfo> requestInfoListAll = new ArrayList();
        for (Phase p: allPhases) {
            requestInfoListAll.addAll(p.getRequestInfoList());
        }

        String csvPath = String.format("output_csv/threads%d_skiers%d_both.csv", info.getNumThreads(), info.getNumSkiers());
        CsvWriter w = new CsvWriter(csvPath, requestInfoListAll);
        w.write();

        System.out.println("\n-------PART 2-------");
        RequestInfoList list = new RequestInfoList(requestInfoListAll);
        list.printReport();
    }

    private static int divideRoundUp(int num, int divider) {
        if (num % divider != 0) return num/divider + 1;
        else return num/divider;
    }
}
