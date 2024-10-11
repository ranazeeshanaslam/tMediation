package com.soft.mediator.beans;

/**
 * <p>Title: Comcerto Mediation Server</p>
 *
 * <p>Description: Meadiation Server</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Comcerto Pvt Ltd</p>
 *
 * @author Muhammad Naveed Alyas
 * @version 1.0
 */
public class CallRad {
    long recordno;
    String username;
    int duration;
    String timeclose;
    String callingnumber;
    String callednumber;
    String confID;
    int callleg;
    String nasipaddress;
    String remoteaddress;
    String remotegatewayid;
    String terminationcause;
    int planID;
    int obaccno;
    String callingID;

    public CallRad() {
    }
    public String check(String input)
    {
        if((input ==null) || (input.length()==0) || (input.trim().length()==0))
            input="00";
        return input.trim();
    }



    public String getCallednumber() {
        return check(callednumber);
    }

    public String getCallingnumber() {
        return check(callingnumber);
    }

    public int getCallleg() {
        return callleg;
    }

    public String getConfID() {
        return check(confID);
    }

    public int getDuration() {
        return duration;
    }

    public String getNasipaddress() {
        return check(nasipaddress);
    }

    public int getObaccno() {
        return obaccno;
    }

    public String getRemoteaddress() {
        return check(remoteaddress);
    }

    public String getRemotegatewayid() {
        return check(remotegatewayid);
    }

    public String getTerminationcause() {
        return check(terminationcause);
    }

    public String getTimeclose() {
        return timeclose;
    }

    public String getUsername() {
        return check(username);
    }

    public long getRecordno() {
        return recordno;
    }

    public int getPlanID() {
        return planID;
    }

    public String getCallingID() {
        return callingID;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public void setTimeclose(String timeclose) {
        this.timeclose = timeclose;
    }

    public void setTerminationcause(String terminationcause) {
        this.terminationcause = terminationcause;
    }

    public void setRemotegatewayid(String remotegatewayid) {
        this.remotegatewayid = remotegatewayid;
    }

    public void setRemoteaddress(String remoteaddress) {
        this.remoteaddress = remoteaddress;
    }

    public void setObaccno(int obaccno) {
        this.obaccno = obaccno;
    }

    public void setNasipaddress(String nasipaddress) {
        this.nasipaddress = nasipaddress;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setConfID(String confID) {
        this.confID = confID;
    }

    public void setCallleg(int callleg) {
        this.callleg = callleg;
    }

    public void setCallingnumber(String callingnumber) {
        this.callingnumber = callingnumber;
    }

    public void setCallednumber(String callednumber) {
        this.callednumber = callednumber;
    }

    public void setRecordno(long recordno) {
        this.recordno = recordno;
    }

    public void setPlanID(int planID) {
        this.planID = planID;
    }

    public void setCallingID(String callingID) {
        this.callingID = callingID;
    }


}
