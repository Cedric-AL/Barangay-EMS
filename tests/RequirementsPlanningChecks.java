import barangayems.backend.*;
import com.google.gson.Gson;
import java.math.BigDecimal;

public final class RequirementsPlanningChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        EvacuationCenter c=new EvacuationCenter();c.id=2;c.capacity=10;
        String[] wheelchair={"Wheelchair access"};
        check(CenterSuitability.assess(c,wheelchair).status().equals("Facility information incomplete"),"Unknown is not unsuitable");
        c.facilityProfileComplete=true;
        check(CenterSuitability.assess(c,wheelchair).status().equals("Some requirements missing"),"Missing facilities");
        c.facilityFeatures=wheelchair;
        check(!CenterSuitability.assess(c,wheelchair).warning(),"Matched facility");
        check(!CenterSuitability.assess(c,new String[0]).warning(),"No selected needs");
        c.occupants=10;check(CenterSuitability.assess(c,wheelchair).warning(),"Full center");c.occupants=0;
        c.archived=true;check(CenterSuitability.assess(c,wheelchair).status().equals("Archived center"),"Archived center");c.archived=false;
        DataSnapshot s=new DataSnapshot();s.centers.add(c);
        SupplyRule rule=new SupplyRule();rule.name="Food packs";rule.unit="packs";rule.group=AllocationPlanner.ALL;
        rule.dailyRate=new BigDecimal("2");rule.days=3;rule.enabled=true;s.supplyRules.add(rule);
        for(int i=0;i<20;i++){Evacuee p=new Evacuee();p.centerId=2L;p.category=i<4?"Child":"None";s.people.add(p);}
        var demand=SupplyDemand.rows(s).get(0);
        check(demand.plan().required()==120&&demand.plan().deficit()==120,"Demand includes missing inventory rows");
        Resource stock=new Resource();stock.centerId=2;stock.name=" food PACKS ";stock.unit="PACKS";stock.available=25;stock.used=5;s.supplies.add(stock);
        Resource source=new Resource();source.centerId=1;source.name="Food packs";source.unit="packs";source.available=200;
        var plan=AllocationPlanner.calculate(s,c,source,rule.group,rule.dailyRate,rule.days);
        check(plan.deficit()==100&&SupplyDemand.rows(s).get(0).plan().deficit()==plan.deficit(),"Planner and report agree");
        check(stock.shortage()==0&&plan.deficit()>0,"Low stock and demand shortage independent");
        for(int i=0;i<80;i++){Evacuee p=new Evacuee();p.centerId=2L;s.people.add(p);}
        rule.dailyRate=new BigDecimal("100000");rule.days=365;
        check(SupplyDemand.rows(s).get(0).plan().required()==3650000000L,"Large coverage avoids integer overflow");
        s.people.subList(20,100).clear();
        rule.group="Child";rule.dailyRate=new BigDecimal("0.125");rule.days=3;
        check(SupplyDemand.rows(s).get(0).plan().required()==2,"Scoped fractional daily rule rounded up");
        rule.enabled=false;check(SupplyDemand.rows(s).isEmpty(),"Disabled rules excluded");rule.enabled=true;
        c.archived=true;check(SupplyDemand.rows(s).isEmpty(),"Archived centers excluded");c.archived=false;
        String json=new Gson().toJson(s);DataSnapshot reloaded=new Gson().fromJson(json,DataSnapshot.class);
        check(reloaded.supplyRules.get(0).dailyRate.equals(new BigDecimal("0.125")),"Rule survives snapshot JSON");
        check(reloaded.centers.get(0).facilityFeatures[0].equals("Wheelchair access"),"Facility profile survives JSON");
        check(SupplyDemand.report(s).contains("Demand shortage"),"Demand report explains result");
        System.out.println("PASS: suitability, full/archived centers, missing inventory demand, shared formula, reserve distinction, group rates, rounding, disabled rules, JSON and reports.");
    }
}
