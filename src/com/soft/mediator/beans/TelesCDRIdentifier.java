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
public class TelesCDRIdentifier {
    String Type;
    int RecordType;
    long SessionID;
    String DateTime;
    String CDRVersion;
    String Daemon;
    String SetID;
    String Name;
    String DaemonStartTime;
    String CallLegID;
    
    String CallingNumber="";
    String CalledNumber="";
    String RedirectNumber="";
    
    String TCallingNumber="";
    String TCalledNumber="";
    String TRedirectNumber="";
    String codec = "";
    
    
    String IngressDNO="";
    String EgressDNO="";
    
    String IngressIP="";
    String EgressIP="";
    String IngressIP1="";
    String EgressIP1="";
    
    long Duration=0;
    String IngressTrunk="";
    String EgressTrunk="";
    String ConnectTime="";
    String DisconnectTime="";
    String Route="";
    int Charge ;
    int RoutePrefixID;
    
    String InDisconnectSwitch = "";
	String InDisconnectCause = "";
    String EgDisconnectSwitch = "";
	String EgDisconnectCause = "";
	
	long PDD = 0;
	long MilliSeconds;
	
	String MSBGi = "";
	String MSBGi_Bytes_In = "";
	String MSBGi_Bytes_Out = "";
	
	String MSBGe = "";
	String MSBGe_Bytes_In = "";
	String MSBGe_Bytes_Out = "";
    
    TelesCDRElement A;
    TelesCDRElement F;
    TelesCDRElement P;
    TelesCDRElement B;
    TelesCDRElement L;
    TelesCDRElement C;
    TelesCDRElement V;
    TelesCDRElement W;
    TelesCDRElement D;
    TelesCDRElement E;
    TelesCDRElement N;
    TelesCDRElement M;
    
    
    public TelesCDRIdentifier() {
    	this.Type="";
    	this.RecordType=0;
    	this.SessionID=0;
    	this.DateTime="";
    	this.CDRVersion="";
    	this.Daemon="";
    	this.SetID="";
    	this.Name="";
    	this.DaemonStartTime="";
    	this.CallLegID="";
    	
    	this.CallingNumber="";
    	this.CalledNumber="";
    	this.RedirectNumber="";
    	this.TCallingNumber="";
    	this.TCalledNumber="";
    	this.TRedirectNumber="";
    	this.codec = "";
    	this.IngressDNO="";
    	this.EgressDNO="";
    	this.IngressIP="";
    	this.EgressIP="";
    	this.IngressIP1="";
    	this.EgressIP1="";
    	this.Duration=0;
    	this.IngressTrunk="";
    	this.EgressTrunk="";
    	this.ConnectTime="";
    	this.DisconnectTime="";
    	this.Route="";
    	this.Charge = 0;
    	this.RoutePrefixID=0;
    	this.InDisconnectSwitch = "";
    	this.InDisconnectCause = "";
    	this.EgDisconnectSwitch = "";
    	this.EgDisconnectCause = "";
        
    	this.PDD = 0;
    	this.MilliSeconds = 0;
    	
    	this.MSBGi = "";
    	this.MSBGi_Bytes_In = "";
    	this.MSBGi_Bytes_Out = "";
    	this.MSBGe = "";
    	this.MSBGe_Bytes_In = "";
    	this.MSBGe_Bytes_Out = "";
    	
    	A = new TelesCDRElement();
    	P = new TelesCDRElement();
    	F = new TelesCDRElement();
    	B = new TelesCDRElement();
    	L = new TelesCDRElement();
    	C = new TelesCDRElement();
    	V = new TelesCDRElement();
    	W = new TelesCDRElement();
    	D = new TelesCDRElement();
    	E = new TelesCDRElement();
    	N = new TelesCDRElement();
    	M = new TelesCDRElement();
    	
    }
    
    public TelesCDRIdentifier(String type, int rectype, long sessionid, String time, String version, String daemon,
    		String setid, String name, String Dstarttime, String Calllegid) {
    	this.Type=type;
    	this.RecordType= rectype;
     	this.SessionID=sessionid;
        this.DateTime=time;
        this.CDRVersion=version;
    	this.Daemon=daemon;
    	this.SetID=setid;
    	this.Name=name;
    	this.DaemonStartTime=Dstarttime;
    	this.CallLegID=Calllegid;
    	
    	this.CallingNumber="";
    	this.CalledNumber="";
    	this.RedirectNumber="";
    	this.TCallingNumber="";
    	this.TCalledNumber="";
    	this.TRedirectNumber="";
    	this.codec = "";
    	this.IngressDNO="";
    	this.EgressDNO="";
    	this.IngressIP="";
    	this.EgressIP="";
    	this.IngressIP1="";
    	this.EgressIP1="";
    	this.Duration=0;
    	this.IngressTrunk="";
    	this.EgressTrunk="";
    	this.ConnectTime="";
    	this.DisconnectTime="";
    	this.Route="";
    	this.Charge = 0;
    	this.RoutePrefixID=0;
    	this.InDisconnectSwitch = "";
    	this.InDisconnectCause = "";
    	this.EgDisconnectSwitch = "";
    	this.EgDisconnectCause = "";
    	
    	this.PDD = 0;
    	this.MilliSeconds = 0;
    	
    	this.MSBGi = "";
    	this.MSBGi_Bytes_In = "";
    	this.MSBGi_Bytes_Out = "";
    	this.MSBGe = "";
    	this.MSBGe_Bytes_In = "";
    	this.MSBGe_Bytes_Out = "";
    	
    	A = new TelesCDRElement();
    	P = new TelesCDRElement();
    	F = new TelesCDRElement();
    	B = new TelesCDRElement();
    	L = new TelesCDRElement();
    	C = new TelesCDRElement();
    	V = new TelesCDRElement();
    	W = new TelesCDRElement();
    	D = new TelesCDRElement();
    	E = new TelesCDRElement();
    	N = new TelesCDRElement();
    	M = new TelesCDRElement();
    	
    }
    
    public String getType() {
        return Type;
    }
    public void setType(String ElementType) {
    	if (ElementType == null) ElementType="";
        this.Type = ElementType;
    }
    
    public int getRecordType() {
        return RecordType;
    }
    public void setRecordType(int RecordType) {
        this.RecordType = RecordType;
    }
    
    public int getCharge() {
        return Charge;
    }
    public void setCharge(int Charge) {
        this.Charge = Charge;
    }
    
    
    
    public long getSessionID() {
        return SessionID;
    }
    public void setSessionID(long SessionID) {
        this.SessionID = SessionID;
    }

    public String getDateTime() {
        return DateTime;
    }
    public void setDateTime(String DateTime) {
    	if (DateTime == null) DateTime="";
        this.DateTime = DateTime;
    }
    
    /*
     * this.CDRVersion=version;
    	this.Daemon=daemon;
    	this.SetID=setid;
    	this.Name=name;
    	this.DaemonStartTime=Dstarttime;
    	this.CallLegID=Calllegid;
     */
    
    public String getCDRVersion() {
        return CDRVersion;
    }
    public void setCDRVersion(String CDRVersion) {
    	if (CDRVersion == null) CDRVersion="";
        this.CDRVersion = CDRVersion;
    }
    
    public String getDaemon() {
        return Daemon;
    }
    public void setDaemon(String Daemon) {
    	if (Daemon == null) Daemon="";
        this.Daemon = Daemon;
    }
    
    public String getSetID() {
        return SetID;
    }
    public void setSetID(String SetID) {
    	if (SetID == null) SetID="";
        this.SetID = SetID;
    }
    
    public String getName() {
        return Name;
    }
    public void setName(String Name) {
    	if (Name == null) Name="";
        this.Name = Name;
    }
    
    public String getDaemonStartTime() {
        return DaemonStartTime;
    }
    public void setDaemonStartTime(String DaemonStartTime) {
    	if (DaemonStartTime == null) DaemonStartTime="";
        this.DaemonStartTime = DaemonStartTime;
    }
    
    public String getCallLegID() {
        return CallLegID;
    }
    public void setCallLegID(String CallLegID) {
    	if (CallLegID == null) CallLegID="";
        this.CallLegID = CallLegID;
    }
    public String getCallingNumber() {
        return CallingNumber;
    }
    public void setCallingNumber(String CallingNumber) {
    	if (CallingNumber == null) CallingNumber="";
        this.CallingNumber = CallingNumber;
    }
    
    public String getCalledNumber() {
        return CalledNumber;
    }
    public void setCalledNumber(String CalledNumber) {
    	if (CalledNumber == null) CalledNumber="";
        this.CalledNumber = CalledNumber;
    }
    public String getRedirectNumber() {
        return RedirectNumber;
    }
    public void setRedirectNumber(String RedirectNumber) {
    	if (RedirectNumber == null) RedirectNumber="";
        this.RedirectNumber = RedirectNumber;
    }
    
    public String getTCallingNumber() {
        return TCallingNumber;
    }
    public void setTCallingNumber(String CallingNumber) {
    	if (CallingNumber == null) CallingNumber="";
        this.TCallingNumber = CallingNumber;
    }
    
    public String getTCalledNumber() {
        return TCalledNumber;
    }
    public void setTCalledNumber(String CalledNumber) {
    	if (CalledNumber == null) CalledNumber="";
        this.TCalledNumber = CalledNumber;
    }
    public String getTRedirectNumber() {
        return TRedirectNumber;
    }
    public void setTRedirectNumber(String RedirectNumber) {
    	if (RedirectNumber == null) RedirectNumber="";
        this.TRedirectNumber = RedirectNumber;
    }
    public void setCodec(String codec){
    	if(codec != null && codec.length() > 0)
    		this.codec = codec;
    	else
    		this.codec = "";
    }
    public String getCodec(){
    	return this.codec;
    }
    
    public String getIngressDNO() {
        return IngressDNO;
    }
    public void setIngressDNO(String IngressDNO) {
    	if (IngressDNO == null) IngressDNO="";
        this.IngressDNO = IngressDNO;
    }
    public String getEgressDNO() {
        return EgressDNO;
    }
    public void setEgressDNO(String EgressDNO) {
    	if (EgressDNO == null) EgressDNO="";
        this.EgressDNO = EgressDNO;
    }
    
    public String getIngressIP() {
        return IngressIP;
    }
    public void setIngressIP(String IngressIP) {
    	if (IngressIP == null) IngressIP="";
        this.IngressIP = IngressIP;
    }
    public String getEgressIP() {
        return EgressIP;
    }
    public void setEgressIP(String EgressIP) {
    	if (EgressIP == null) EgressIP="";
        this.EgressIP = EgressIP;
    }
    
    public String getIngressIP1() {
        return IngressIP1;
    }
    public void setIngressIP1(String IngressIP) {
    	if (IngressIP == null) IngressIP="";
        this.IngressIP1 = IngressIP;
    }
    public String getEgressIP1() {
        return EgressIP1;
    }
    public void setEgressIP1(String EgressIP) {
    	if (EgressIP == null) EgressIP="";
        this.EgressIP1 = EgressIP;
    }
    
    
    public String getIngressTrunk() {
        return IngressTrunk;
    }
    public void setIngressTrunk(String IngressTrunk) {
    	if (IngressTrunk == null) IngressTrunk="";
        this.IngressTrunk = IngressTrunk;
    }
    
    public String getEgressTrunk() {
        return EgressTrunk;
    }
    public void setEgressTrunk(String EgressTrunk) {
    	if (EgressTrunk == null) EgressTrunk="";
        this.EgressTrunk = EgressTrunk;
    }
    
    public long getDuration() {
        return Duration;
    }
    public void setDuration(long Duration) {
        this.Duration = Duration;
    }
        
    public String getConnectTime() {
        return ConnectTime;
    }
    public void setConnectTime(String ConnectTime) {
    	if (ConnectTime == null) ConnectTime="";
        this.ConnectTime = ConnectTime;
    }
    
    public String getDisconnectTime() {
        return DisconnectTime;
    }
    public void setDisconnectTime(String DisconnectTime) {
    	if (DisconnectTime == null) DisconnectTime="";
        this.DisconnectTime = DisconnectTime;
    }
   
    public String getRoute() {
        return Route;
    }
    public void setRoute(String Route) {
    	if (Route == null) Route="";
        this.Route = Route;
    }
    /*
     * this.InDisconnectSwitch = "";
    	this.InDisconnectCause = "";
    	this.egDisconnectSwitch = "";
    	this.egDisconnectCause = "";
     */
    
    public String getInDisconnectSwitch() {
        return InDisconnectSwitch;
    }
    public void setInDisconnectSwitch(String InDisconnectSwitch) {
    	if (InDisconnectSwitch == null) InDisconnectSwitch="";
        this.InDisconnectSwitch = InDisconnectSwitch;
    }
    
    public String getInDisconnectCause() {
        return InDisconnectCause;
    }
    public void setInDisconnectCause(String InDisconnectCause) {
    	if (InDisconnectCause == null) InDisconnectCause="";
        this.InDisconnectCause = InDisconnectCause;
    }
    
    public String getEgDisconnectSwitch() {
        return EgDisconnectSwitch;
    }
    public void setEgDisconnectSwitch(String EgDisconnectSwitch) {
    	if (EgDisconnectSwitch == null) EgDisconnectSwitch="";
        this.EgDisconnectSwitch = EgDisconnectSwitch;
    }
    
    public String getEgDisconnectCause() {
        return EgDisconnectCause;
    }
    public void setEgDisconnectCause(String EgDisconnectCause) {
    	if (EgDisconnectCause == null) EgDisconnectCause="";
        this.EgDisconnectCause = EgDisconnectCause;
    }
    
    public int getRoutePrefixID() {
        return RoutePrefixID;
    }
    public void setRoutePrefixID(int id) {
        this.RoutePrefixID = id;
    }
    
    
    /*
     * A = new TelesCDRElement();
    	P = new TelesCDRElement();
    	F = new TelesCDRElement();
    	B = new TelesCDRElement();
    	
     */
    
    public TelesCDRElement getElementA() {
        return A;
    }
    public void setElementA(TelesCDRElement el) {
    	this.A= el;
    }
    
    public TelesCDRElement getElementP() {
        return P;
    }
    public void setElementP(TelesCDRElement el) {
    	this.P= el;
    }
    
    public TelesCDRElement getElementF() {
        return F;
    }
    public void setElementF(TelesCDRElement el) {
    	this.F= el;
    }
    
    public TelesCDRElement getElementB() {
        return B;
    }
    public void setElementB(TelesCDRElement el) {
    	this.B= el;
    }
    /*
     * L = new TelesCDRElement();
    	C = new TelesCDRElement();
    	V = new TelesCDRElement();
    	W = new TelesCDRElement();
    	  */
    
    public TelesCDRElement getElementL() {
        return L;
    }
    public void setElementL(TelesCDRElement el) {
    	this.L= el;
    }
    
    public TelesCDRElement getElementC() {
        return C;
    }
    public void setElementC(TelesCDRElement el) {
    	this.C= el;
    }
    
    public TelesCDRElement getElementV() {
        return V;
    }
    public void setElementV(TelesCDRElement el) {
    	this.V= el;
    }
    
    public TelesCDRElement getElementW() {
        return W;
    }
    public void setElementW(TelesCDRElement el) {
    	this.W= el;
    }
    
    /*
     * D = new TelesCDRElement();
    	E = new TelesCDRElement();
    	N = new TelesCDRElement();
    	M = new TelesCDRElement();
   
     */
    public TelesCDRElement getElementD() {
        return D;
    }
    public void setElementD(TelesCDRElement el) {
    	this.D= el;
    }
    
    public TelesCDRElement getElementE() {
        return E;
    }
    public void setElementE(TelesCDRElement el) {
    	this.E= el;
    }
    
    public TelesCDRElement getElementN() {
        return N;
    }
    public void setElementN(TelesCDRElement el) {
    	this.N= el;
    }
    
    public TelesCDRElement getElementM() {
        return M;
    }
    public void setElementM(TelesCDRElement el) {
    	this.M= el;
    }
    
	public long getPDD() {
		return PDD;
	}

	public void setPDD(long pDD) {
		PDD = pDD;
	}

	public String getMSBGi() {
		return MSBGi;
	}

	public void setMSBGi(String mSBGi) {
		MSBGi = mSBGi;
	}

	public String getMSBGi_Bytes_In() {
		return MSBGi_Bytes_In;
	}

	public void setMSBGi_Bytes_In(String mSBGi_Bytes_In) {
		MSBGi_Bytes_In = mSBGi_Bytes_In;
	}

	public String getMSBGi_Bytes_Out() {
		return MSBGi_Bytes_Out;
	}

	public void setMSBGi_Bytes_Out(String mSBGi_Bytes_Out) {
		MSBGi_Bytes_Out = mSBGi_Bytes_Out;
	}

	public String getMSBGe() {
		return MSBGe;
	}

	public void setMSBGe(String mSBGe) {
		MSBGe = mSBGe;
	}

	public String getMSBGe_Bytes_In() {
		return MSBGe_Bytes_In;
	}

	public void setMSBGe_Bytes_In(String mSBGe_Bytes_In) {
		MSBGe_Bytes_In = mSBGe_Bytes_In;
	}

	public String getMSBGe_Bytes_Out() {
		return MSBGe_Bytes_Out;
	}

	public void setMSBGe_Bytes_Out(String mSBGe_Bytes_Out) {
		MSBGe_Bytes_Out = mSBGe_Bytes_Out;
	}

	public long getMilliSeconds() {
		return MilliSeconds;
	}

	public void setMilliSeconds(long milliSeconds) {
		MilliSeconds = milliSeconds;
	}
}
