package com.soft.mediator.exceptions; 

/**
 * <p>Title: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Comcerto (pvt) Ltd.</p>
 *
 * @author Afzaal
 *
 * @version 1.0
 **/

public class ObjectAlreadyExistException extends Exception {
    public ObjectAlreadyExistException() {
    }
    public ObjectAlreadyExistException(String message) {
        super(message);
    }
}
