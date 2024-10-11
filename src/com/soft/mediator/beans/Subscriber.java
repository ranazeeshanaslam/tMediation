package com.soft.mediator.beans;

/**
 * <p>Title: Terminus Billing System</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Comcerto (pvt) Ltd.</p>
 *
 * @author Naveed Alyas
 *
 * @version 1.0
 **/
public class Subscriber {

    private int subscriberID;
    private int accountNo;
    private double creditLimit;
    private double usedLimit;
    private int PackageID;
    private double PG_CostOfSale;
    private double LineRent;
    private double CostOfSale;
    private int productID;
    private int BillingPeriodID;
    private String SI_SubsIdentification;
    private int SV_ServiceId;
    private int SI_ServiceInstanceId;

    private String rateConf;
    private double usageLimit;
    private int chargingType;
    private double volumeCharged;
    private double amountCharged;
    private int accountType;

    public Subscriber() {
    }

    public Subscriber(int subscriberID,
                      int  accountNo,
                      double creditLimit,
                      double usedLimit,
                      int  PackageID,
                      double LineRent,
                      double CostOfSale, 
                      int BillingPeriodID,
                      int productID,
                      String SI_SubsIdentification,
                      int SV_ServiceId,
                      int SI_ServiceInstanceId,

                      String rateConf,
                      double usageLimit,
                      int chargingType,
                      double volumeCharged,
                      double amountCharged,
                      int accountType
           ) {
        this.subscriberID = subscriberID;
        this.accountNo = accountNo;
        this.creditLimit = creditLimit;
        this.usedLimit = usedLimit;
        this.PackageID = PackageID;
        this.LineRent = LineRent;
        this.CostOfSale = CostOfSale;
        this.BillingPeriodID=BillingPeriodID;
        this.productID = productID;
        this.SI_SubsIdentification = SI_SubsIdentification;
        this.SV_ServiceId = SV_ServiceId;
        this.SI_ServiceInstanceId = SI_ServiceInstanceId;

        this.rateConf = rateConf;
        this.usageLimit = usageLimit;
        this.chargingType = chargingType;
        this.volumeCharged = volumeCharged;
        this.amountCharged = amountCharged;
        this.accountType = accountType;
    }

    public int getAccountNo() {
        return this.accountNo;
    }
    public int getsubscriberID() {
        return this.subscriberID;
    }
    public double getCreditLimit() {
        return this.creditLimit;
    }
    public double getusedLimit() {
        return this.usedLimit;
    }
    public double getLineRent() {
        return this.LineRent;
    }
    public double getCostOfSale() {
        return this.CostOfSale;
    }
    public int getPackageID() {
        return this.PackageID;
    }
    public int getproductID() {
        return this.productID;
    }
    public int getBillingPeriodID() {
        return this.BillingPeriodID;
    }
    public String getSI_SubsIdentification(){
        return this.SI_SubsIdentification;
    }
    public int getSV_ServiceId(){
        return this.SV_ServiceId;
    }
    public int getSI_ServiceInstanceId(){
       return this.SI_ServiceInstanceId;
    }
    public String getRateConf(){
        return this.rateConf;
    }
    public double getUsageLimit(){
        return this.usageLimit;
    }
    public int getChargingType(){
        return this.chargingType;
    }
    public double getVolumeCharged(){
        return this.volumeCharged;
    }
    public double getAmountCharged(){
        return this.amountCharged;
    }

    public int getAccountType(){
        return this.accountType;
    }


    public void setAccountNo(int accountNo) {
        this.accountNo= accountNo;
    }
    public void setsubscriberID(int subscriberID) {
        this.subscriberID=subscriberID;
    }
    public void setCreditLimit(double CreditLimit) {
        this.creditLimit=CreditLimit;
    }
    public void setUsedLimit(double usedLimit) {
        this.usedLimit=usedLimit;
    }
    public void setLineRent(double LineRent) {
        this.LineRent=LineRent;
    }
    public void setCostOfSale(double CostOfSale) {
        this.CostOfSale=CostOfSale;
    }
    public void setPackageID(int PackageID) {
        this.PackageID=PackageID;
    }
    public void setproductID(int productID) {
        this.productID=productID;
    }
    public void setBillingPeriodID(int BillingPeriodID) {
        this.BillingPeriodID=BillingPeriodID;
    }
    public void setSI_SubsIdentification(String SI_SubsIdentification){
        this.SI_SubsIdentification = SI_SubsIdentification;
    }
    public void setSV_ServiceId(int SV_ServiceId){
        this.SV_ServiceId = SV_ServiceId;
    }

    public void setSI_ServiceInstanceId(int SI_ServiceInstanceId){
        this.SI_ServiceInstanceId = SI_ServiceInstanceId;
    }
    public void setRateConf(String rateConf){
        this.rateConf = rateConf;
    }
    public void setUsageLimit(double usageLimit){
        this.usedLimit = usageLimit;
    }
    public void setChargingType(int chargingType){
        this.chargingType = chargingType;
    }
    public void setVolumeCharged(double volumeCharged){
        this.volumeCharged = volumeCharged;
    }
    public void setAmountCharged(double amountCharged){
        this.amountCharged = amountCharged;
    }
    public void setAccountType(int accountType){
        this.accountType = accountType;
    }

}
