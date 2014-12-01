package pondero.task;

import pondero.task.responses.Response;
import pondero.tests.elements.other.Instruct;

public interface Testable {

    void doStep(Response input) throws Exception;

    String getCodeName();

    Instruct getInstructions();

    void kill();

}