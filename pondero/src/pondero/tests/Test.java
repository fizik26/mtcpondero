package pondero.tests;

import java.awt.EventQueue;
import java.util.List;
import java.util.Stack;
import pondero.data.Workbook;
import pondero.data.evaluation.scoring.Evaluation;
import pondero.data.model.basic.Participant;
import pondero.data.model.basic.TestInstance;
import pondero.data.model.basic.TrialRecord;
import pondero.task.Testable;
import pondero.task.launch.DefaultMonitor;
import pondero.task.launch.TaskData;
import pondero.task.launch.TaskMonitor;
import pondero.task.responses.Response;
import pondero.tests.elements.other.Block;
import pondero.tests.elements.trial.Trial;
import pondero.tests.interfaces.HasScreencolor;
import pondero.tests.interfaces.IsController;
import pondero.tests.interfaces.HasFeedback.FeedbackStimulus;
import pondero.ui.exceptions.ExceptionReporting;

public abstract class Test extends TestRenderer implements IsController, Testable {

    private Workbook                  workbook;
    private Participant               participant;
    private TaskData                  taskData;
    private TaskMonitor               taskMonitor;
    private TrialRecord               record;

    private final Stack<IsController> controllerStack = new Stack<IsController>();
    private Block                     currentBlock;

    public void closeRecord() {
        if (record != null) {
            taskData.add(record);
            record = null;
        }
    }

    public TrialRecord createRecord(final long runId) throws Exception {
        TrialRecord record = null;
        if (workbook != null) {
            record = workbook.addTrialRecord(getTestId());
            record.setExperimentId(runId);
        }
        return record;
    }

    @Override
    public synchronized void doBegin() throws Exception {
        controllerStack.clear();
        taskData = new TaskData(System.currentTimeMillis());
        taskData.markStartTime();
        taskMonitor.onTaskStarted(this);
        if (getExperiment() != null) {
            getExperiment().doBegin();
        } else if (blocks.size() > 0) {
            blocks.values().iterator().next().doBegin();
        } else if (trials.size() > 0) {
            trials.values().iterator().next().doBegin();
        }
    }

    @Override
    public synchronized void doEnd() {
        taskData.markStopTime(TaskData.END_SUCCESS);
        getTestWindow().hideTestWindow();
        taskMonitor.onTaskEnded(this, taskData);
    }

    @Override
    public synchronized void doStep(final Response input) throws Exception {
        final IsController controller = peekController();
        if (controller == null) {
            doEnd();
        } else {
            controller.doStep(input);
        }
    }

    public List<String> getBgstim() {
        if (currentBlock != null) { return currentBlock.getBgstim(); }
        return null;
    }

    public FeedbackStimulus getCorrectmessage() {
        if (currentBlock != null) { return currentBlock.getCorrectMessage(); }
        return null;
    }

    public FeedbackStimulus getErrormessage() {
        if (currentBlock != null) { return currentBlock.getErrorMessage(); }
        return null;
    }

    public Evaluation getEvaluation(final TestInstance instance) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void kill() {
        taskData.markStopTime(TaskData.END_USER);
        taskMonitor.onTaskEnded(this, taskData);
    }

    public void openRecord(final Trial trial) throws Exception {
        record = createRecord(taskData.getRunId());
        if (record != null) {
            if (participant != null) {
                record.setParticipant(participant);
            }
            if (currentBlock != null) {
                record.setBlockId(currentBlock.getName());
            }
            if (trial != null) {
                record.setTrialId(trial.getName());
            }
        }
    }

    public IsController peekController() {
        IsController controller = null;
        if (!controllerStack.isEmpty()) {
            controller = controllerStack.peek();
        }
        return controller;
    }

    public void popController() throws Exception {
        final IsController controller = controllerStack.pop();
        if (controller instanceof Block) {
            currentBlock = null;
        }
        if (controller instanceof HasScreencolor) {
            popScreencolor();
        }
        doStep(null);
    }

    public void pushController(final IsController controller) {
        controllerStack.push(controller);
        if (controller instanceof Block) {
            currentBlock = (Block) controller;
        }
        if (controller instanceof HasScreencolor) {
            pushScreencolor((HasScreencolor) controller);
        }
    }

    public void recordCorrectResponse(final long time) throws Exception {
        if (record != null) {
            record.setResponseTimestamp(time);
            record.setResponseCorrect(true);
        }
    }

    public void recordErrorResponse(final long time) throws Exception {
        if (record != null) {
            record.setResponseTimestamp(time);
            record.setResponseCorrect(false);
        }
    }

    public void recordResponse(final String response) throws Exception {
        if (record != null) {
            record.setResponse(response);
        }
    }

    @Override
    public void run() {
        try {
            pushScreencolor(getDefaults());
            getTestWindow().showTestWindow();
            doBegin();
            doStep(null);
        } catch (final Exception e) {
            taskMonitor.onTaskEnded(this, taskData);
            ExceptionReporting.showExceptionMessage(null, e);
        }
    }

    public void setParticipant(final Participant participant) {
        this.participant = participant;
    }

    public void setWorkbook(final Workbook workbook) {
        this.workbook = workbook;
    }

    public void start(final TaskMonitor launcher) {
        taskMonitor = launcher == null ? new DefaultMonitor() : launcher;
        EventQueue.invokeLater(this);
    }

}