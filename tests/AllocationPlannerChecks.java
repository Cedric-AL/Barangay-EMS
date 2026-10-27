import barangayems.backend.*;
import java.math.BigDecimal;

public final class AllocationPlannerChecks {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        DataSnapshot s = new DataSnapshot();
        EvacuationCenter c = new EvacuationCenter(); c.id = 2;
        Resource source = new Resource(); source.centerId = 1L; source.name = "Food";
        source.unit = "packs"; source.available = 100; source.used = 10;
        Resource local = new Resource(); local.centerId = 2L; local.name = " food ";
        local.unit = "PACKS"; local.available = 15; local.used = 5; s.supplies.add(local);
        Resource otherUnit = new Resource(); otherUnit.centerId = 2L; otherUnit.name = "Food";
        otherUnit.unit = "boxes"; otherUnit.available = 100; s.supplies.add(otherUnit);
        for (int i=0;i<20;i++) { Evacuee p=new Evacuee(); p.centerId=2L;
            p.category=i<5?"Child":"None"; s.people.add(p); }
        Evacuee elsewhere=new Evacuee(); elsewhere.centerId=3L; s.people.add(elsewhere);
        AllocationPlanner.Plan p=AllocationPlanner.calculate(s,c,source,AllocationPlanner.ALL,new BigDecimal("2"),3);
        check(p.people()==20 && p.required()==120 && p.onHand()==10,"Population, days, and matching stock");
        check(p.deficit()==110 && p.suggested()==90 && p.unmet()==20,"Source stock cap and unmet need");
        p=AllocationPlanner.calculate(s,c,source,"Child",new BigDecimal("0.75"),3);
        check(p.people()==5 && p.required()==12 && p.suggested()==2,"Category scope and round up");
        p=AllocationPlanner.calculate(s,c,source,"Pregnant",BigDecimal.ONE,1);
        check(p.required()==0 && p.suggested()==0,"Empty target group");
        source.available=10;
        p=AllocationPlanner.calculate(s,c,source,AllocationPlanner.ALL,BigDecimal.ONE,1);
        check(p.suggested()==0 && p.unmet()==10,"Empty source");
        for (String rate : new String[]{"0","-1","999999999999999999"}) {
            try { AllocationPlanner.calculate(s,c,source,AllocationPlanner.ALL,new BigDecimal(rate),1);
                throw new AssertionError("Invalid rate accepted"); } catch (IllegalArgumentException expected) {}
        }
        source.centerId=2L;
        try { AllocationPlanner.calculate(s,c,source,AllocationPlanner.ALL,BigDecimal.ONE,1);
            throw new AssertionError("Same center accepted"); } catch (IllegalArgumentException expected) {}
        System.out.println("PASS: demand, destination stock, unit matching, category selection, rounding, stock limits, empty groups, invalid rates and same-center rejection.");
    }
}
