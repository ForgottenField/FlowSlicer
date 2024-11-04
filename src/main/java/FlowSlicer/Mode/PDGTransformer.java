package FlowSlicer.Mode;

import FlowSlicer.Global;
import FlowSlicer.Statistics;
import fj.P;
import heros.solver.CountingThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.options.Options;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class PDGTransformer extends BodyTransformer {
    private int max = 0;
    private int counter = 0;
    private HashSet<String> classSet;
    private final Lock lock = new ReentrantLock();

    public PDGTransformer(HashSet<String> classSet) {
        super();
        this.classSet = classSet;
    }

    public PDGTransformer() {
        super();
        this.classSet = new HashSet<>();
    }

    @Override
    /***
     * Transform method body into PDG
     */
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
//        if (classSet.contains(b.getMethod().getDeclaringClass().getName())) {
////            log.info("internalTransform called for method: " + b.getMethod().getSignature() + ", phase: " + phaseName);
//
//            // return if the method does not have body
////            if(!b.getMethod().isConcrete() || !b.getMethod().getDeclaringClass().isConcrete())
////                return;
//
//        }

//        synchronized (lock) {
//            if (classSet.contains(b.getMethod().getDeclaringClass().getName())) {
//                final DependenceGraph pdg = PDGHelper.getPDG(b);
//                int num = Global.v().getAppModel().getGlobalStmtNum();
//                Global.v().getAppModel().setGlobalStmtNum(num + pdg.getAllNodes().size() - 1);
//                Packs.getInstance().addPdg(b.getMethod(), pdg);
//            }
//        }

        int threadNum = Options.v().num_threads();
        if (threadNum < 1) {
            threadNum = Runtime.getRuntime().availableProcessors();
        }
        CountingThreadPoolExecutor executor =
                new CountingThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        executor.execute(() -> {
            if (classSet.contains(b.getMethod().getDeclaringClass().getName())) {
                final DependenceGraph pdg = PDGHelper.getPDG(b);
                Packs.getInstance().addPdg(b.getMethod(), pdg);

                synchronized (lock) {
                    int num = Global.v().getAppModel().getGlobalStmtNum();
                    Global.v().getAppModel().setGlobalStmtNum(num + pdg.getAllNodes().size() - 1);
                }
            }
        });

        try {
            executor.awaitCompletion();
            executor.shutdown();
        } catch (InterruptedException e) {
            // Something went horribly wrong
            throw new RuntimeException("Could not wait for pack threads to finish: " + e.getMessage(), e);
        }

        Throwable exception = executor.getException();
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }

//        if (b.getMethod().getDeclaringClass().getName().equals("com.appbrain.a.bd$2")) {
//            synchronized (lock) {
//                this.counter++;
//                // Get PDG form method body
//                final DependenceGraph pdg = PDGHelper.getPDG(b);
//                Packs.getInstance().addPdg(b.getMethod(), pdg);
//
////                log.info(b.getMethod().getSignature() + " building successfully!");
//            }
//        }
    }
}
