package pondero.task;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import pondero.data.Workbook;
import pondero.data.model.basic.Participant;
import pondero.task.controllers.BlockController;
import pondero.task.controllers.ExperimentController;
import pondero.task.controllers.PageController;
import pondero.task.controllers.RootController;
import pondero.task.controllers.TaskController;
import pondero.task.controllers.TrialController;
import pondero.task.launch.TaskData;
import pondero.task.launch.TaskMonitor;
import pondero.task.launch.TaskRenderer;
import pondero.tests.Test;
import pondero.tests.elements.workflow.Block;
import pondero.tests.elements.workflow.Experiment;

public abstract class TaskBase extends Thread implements Iterable<TaskController> {

    private final TaskRenderer         renderer;
    private final Test                 test;
    private final Workbook             workbook;
    private final Participant          participant;
    private final TaskData             data     = new TaskData(System.currentTimeMillis()); ;
    private final List<TaskMonitor>    monitors = new ArrayList<>();
    private final List<TaskController> leaves   = new ArrayList<>();

    protected TaskBase(final TaskRenderer renderer, final Test test) {
        this(renderer, test, null);
    }

    protected TaskBase(final TaskRenderer renderer, final Test test, final Workbook workbook) {
        this(renderer, test, workbook, null);
    }

    protected TaskBase(final TaskRenderer renderer, final Test test, final Workbook workbook, final Participant participant) {
        this.renderer = renderer;
        this.workbook = workbook;
        this.participant = participant;
        this.test = test;
        buildExperimentTree();
    }

    public void addMonitor(final TaskMonitor monitor) {
        monitors.add(monitor);
    }

    public String getCodeName() {
        return test.getCodeName();
    }

    public TaskData getData() {
        return data;
    }

    public Participant getParticipant() {
        return participant;
    }

    public Test getTest() {
        return test;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    @Override
    public ListIterator<TaskController> iterator() {
        return leaves.listIterator();
    }

    protected void buildExperimentTree() {
        final Experiment experiment = test.getExperiment();
        final TaskController experimentController = new ExperimentController((Task) this, experiment);
        if (experiment.getPreinstructions() != null) {
            for (final String pageId : experiment.getPreinstructions()) {
                final PageController pageController = new PageController((Task) this, test.getPage(pageId));
                experimentController.addChild(pageController);
                leaves.add(pageController);
            }
        }
        for (final String blockId : experiment.getBlocks()) {
            final Block block = test.getBlock(blockId);
            final BlockController blockController = new BlockController((Task) this, block);
            experimentController.addChild(blockController);
            if (block.getPreinstructions() != null) {
                for (final String pageId : block.getPreinstructions()) {
                    final PageController pageController = new PageController((Task) this, test.getPage(pageId));
                    blockController.addChild(pageController);
                    leaves.add(pageController);
                }
            }
            for (final String trialId : block.getTrials()) {
                final TrialController trialController = new TrialController((Task) this, test.getTrial(trialId));
                blockController.addChild(trialController);
                leaves.add(trialController);
            }
            if (block.getPostinstructions() != null) {
                for (final String pageId : block.getPostinstructions()) {
                    final PageController pageController = new PageController((Task) this, test.getPage(pageId));
                    blockController.addChild(pageController);
                    leaves.add(pageController);
                }
            }
        }
        if (experiment.getPostinstructions() != null) {
            for (final String pageId : experiment.getPostinstructions()) {
                final PageController pageController = new PageController((Task) this, test.getPage(pageId));
                experimentController.addChild(pageController);
                leaves.add(pageController);
            }
        }
        final RootController rootController = new RootController((Task) this, test.getDefaults(), test.getInstructions());
        rootController.addChild(experimentController);
    }

    protected synchronized void cleanUp(final int errorCode) {
        data.markStopTime(errorCode);
        renderer.doEnd();
        signalTaskEnded();
        interrupt();
    }

    protected TaskRenderer getRenderer() {
        return renderer;
    }

    protected void signalTaskEnded() {
        for (final TaskMonitor monitor : monitors) {
            monitor.onTaskEnded(test, data);
        }
    }

    protected void signalTaskStarted() {
        for (final TaskMonitor monitor : monitors) {
            monitor.onTaskStarted(test);
        }
    }

}
