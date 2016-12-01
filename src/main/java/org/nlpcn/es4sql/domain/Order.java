package org.nlpcn.es4sql.domain;

/**
 * 排序规则
 * @author ansj
 *
 */
public class Order {
	private String name;
	private String type;
	private boolean isNested = false;
	private String mode;
	private String path;
	private Where condition;

	public Order(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public Order(boolean isNested, String mode, String path,
                 Where condition, String name, String type) {
		this.isNested = isNested;
		this.mode = mode;
		this.path = path;
		this.condition = condition;
		this.name = name;
		this.type = type;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isNested() {
		return isNested;
	}

	public void setNested(boolean nested) {
		isNested = nested;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Where getCondition() {
		return condition;
	}

	public void setCondition(Where condition) {
		this.condition = condition;
	}
}
