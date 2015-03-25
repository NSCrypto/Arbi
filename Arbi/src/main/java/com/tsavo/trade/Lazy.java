package com.tsavo.trade;


public class Lazy<T> {

	T t;
	LazyFactory<T> con;
	
	public Lazy(LazyFactory<T> con) {
		this.con = con;
	}

	public T get(){
		if(t == null){
			t = con.get();
		}
		return t;
	}
	
	public interface LazyFactory<T>{
		public T get();
	}
}
