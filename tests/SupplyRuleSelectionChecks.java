package barangayems.frontend;
import barangayems.backend.*;
import java.math.BigDecimal;
public class SupplyRuleSelectionChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        DataSnapshot s=new DataSnapshot();
        check(SupplyRules.inventoryItems(s).length==0,"No invented items in empty inventory");
        for(String[] row:new String[][]{{"Water","bottles"},{" water ","BOTTLES"},{"Water","liters"}}){
            Resource r=new Resource();r.name=row[0];r.unit=row[1];s.supplies.add(r);
        }
        var items=SupplyRules.inventoryItems(s);
        check(items.length==2,"Deduplicate across centers but keep different units");
        check(items[0].name().equals("Water")&&items[0].unit().equals("bottles"),"Preserve item and unit together");
        SupplyRule r=new SupplyRule();
        for(String[] example:new String[][]{{"3.0000","3"},{"0.1250","0.125"},{"1000.0000","1000"},{"2.5000","2.5"}}){
            r.dailyRate=new BigDecimal(example[0]);check(r.rateText().equals(example[1]),"Readable decimal rate");
            check(r.dailyRate.compareTo(new BigDecimal(example[0]))==0,"Display does not change precision");
        }
        System.out.println("PASS: existing inventory choices, deduplication, paired units and daily rate display.");
    }
}
