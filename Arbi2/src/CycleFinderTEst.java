

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.tsavo.trade.opportunity.cycle.MarketCycle;
import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinderTEst {

	@Test
	public void testFindCycles() {
		Set<CurrencyPair> pairs = new HashSet<>();
		pairs.add(new CurrencyPair("BTC", "USD"));
		pairs.add(new CurrencyPair("DOGE", "USD"));
		pairs.add(new CurrencyPair("DRK", "USD"));
		pairs.add(new CurrencyPair("FTC", "USD"));
		pairs.add(new CurrencyPair("LTC", "USD"));
		pairs.add(new CurrencyPair("NXT", "USD"));
		pairs.add(new CurrencyPair("PPC", "USD"));
		pairs.add(new CurrencyPair("RDD", "USD"));
		pairs.add(new CurrencyPair("XPY", "USD"));
		pairs.add(new CurrencyPair("XRP", "USD"));
		
		pairs.add(new CurrencyPair("42", "BTC"));
		pairs.add(new CurrencyPair("DOGE", "BTC"));
		pairs.add(new CurrencyPair("FTC", "BTC"));
		pairs.add(new CurrencyPair("LTC", "BTC"));
		pairs.add(new CurrencyPair("DRK", "BTC"));
		pairs.add(new CurrencyPair("PPC", "BTC"));
		pairs.add(new CurrencyPair("NXT", "BTC"));
		pairs.add(new CurrencyPair("XRP", "BTC"));
		
		pairs.add(new CurrencyPair("DOGE", "LTC"));
		pairs.add(new CurrencyPair("DRK", "LTC"));
		pairs.add(new CurrencyPair("FTC", "LTC"));
		pairs.add(new CurrencyPair("NXT", "LTC"));
		
		pairs.add(new CurrencyPair("PPC", "LTC"));
		pairs.add(new CurrencyPair("RDD", "LTC"));
		pairs.add(new CurrencyPair("XRP", "LTC"));
		pairs.add(new CurrencyPair("42", "XRP"));
		pairs.add(new CurrencyPair("DOGE", "XRP"));
		pairs.add(new CurrencyPair("DRK", "XRP"));
		pairs.add(new CurrencyPair("FTC", "XRP"));
		pairs.add(new CurrencyPair("LTC", "XRP"));
		pairs.add(new CurrencyPair("NXT", "XRP"));
		pairs.add(new CurrencyPair("RDD", "XRP"));
		
		
		
		CycleFinder finder = new CycleFinder();
		Set<MarketCycle> cycles = finder.findCycles(pairs);
		cycles.stream().sorted((x,y) -> y.markets.size() - x.markets.size()).forEach(x -> System.out.println(x));
		System.out.println(cycles.stream().filter(x -> x.markets.size() < 5).collect(Collectors.toList()).size());
	}

}
