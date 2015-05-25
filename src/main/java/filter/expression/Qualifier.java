package filter.expression;

import backend.interfaces.IModel;
import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import backend.resource.TurboMilestone;
import backend.resource.TurboUser;
import filter.MetaQualifierInfo;
import filter.QualifierApplicationException;
import util.Utility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Qualifier implements FilterExpression {

	public static final Qualifier EMPTY = new Qualifier("", "");

	private final String name;

	// Only one of these will be present at a time
	private Optional<DateRange> dateRange = Optional.empty();
	private Optional<String> content = Optional.empty();
	private Optional<LocalDate> date = Optional.empty();
	private Optional<NumberRange> numberRange = Optional.empty();
	private Optional<Integer> number = Optional.empty();

	// Copy constructor
	public Qualifier(Qualifier other) {
		this.name = other.getName();
		if (other.getDateRange().isPresent()) {
			this.dateRange = other.getDateRange();
		} else if (other.getDate().isPresent()) {
			this.date = other.getDate();
		} else if (other.getContent().isPresent()) {
			this.content = other.getContent();
		} else if (other.getNumberRange().isPresent()) {
			this.numberRange = other.getNumberRange();
		} else if (other.getNumber().isPresent()) {
			this.number = other.getNumber();
		} else {
			assert false : "Unrecognised content type! You may have forgotten to add it above";
		}
	}

	public Qualifier(String name, String content) {
		this.name = name;
		this.content = Optional.of(content);
	}

	public Qualifier(String name, NumberRange numberRange) {
		this.name = name;
		this.numberRange = Optional.of(numberRange);
	}

	public Qualifier(String name, DateRange dateRange) {
		this.name = name;
		this.dateRange = Optional.of(dateRange);
	}

	public Qualifier(String name, LocalDate date) {
		this.name = name;
		this.date = Optional.of(date);
	}

	public Qualifier(String name, int number) {
		this.name = name;
		this.number = Optional.of(number);
	}

	/**
	 * Helper function for testing a filter expression against an issue.
	 * Ensures that meta-qualifiers are taken care of.
	 * Should always be used over isSatisfiedBy.
	 */
	public static boolean process(IModel model, FilterExpression expr, TurboIssue issue) {

		FilterExpression exprWithNormalQualifiers = expr.filter(Qualifier::shouldNotBeStripped);
		List<Qualifier> metaQualifiers = expr.find(Qualifier::isMetaQualifier);

		// Preprocessing for repo qualifier
		boolean containsRepoQualifier = metaQualifiers.stream()
				.map(Qualifier::getName)
				.collect(Collectors.toList())
			.contains("repo");

		if (!containsRepoQualifier) {
			exprWithNormalQualifiers = new Conjunction(
				new Qualifier("repo", model.getDefaultRepo()),
				exprWithNormalQualifiers);
		}

		return exprWithNormalQualifiers.isSatisfiedBy(model, issue, new MetaQualifierInfo(metaQualifiers));
	}

	public static void processMetaQualifierEffects(FilterExpression expr, Consumer<Qualifier> callback) {
		expr.find(Qualifier::isMetaQualifier).forEach(callback);
	}

	private static LocalDateTime currentTime = null;

	private static LocalDateTime getCurrentTime() {
		if (currentTime == null) {
			return LocalDateTime.now();
		} else {
			return currentTime;
		}
	}

	/**
	 * For testing. Stubs the current time so time-related qualifiers work properly.
	 */
	public static void setCurrentTime(LocalDateTime dateTime) {
		currentTime = dateTime;
	}

	public boolean isEmptyQualifier() {
		return name.isEmpty() && content.isPresent() && content.get().isEmpty();
	}

	@Override
    public boolean isSatisfiedBy(IModel model, TurboIssue issue, MetaQualifierInfo info) {
        assert name != null && content != null;

        // The empty qualifier is satisfied by anything
        if (isEmptyQualifier()) return true;

        switch (name) {
        case "id":
            return idSatisfies(issue);
        case "keyword":
            return keywordSatisfies(issue, info);
        case "title":
            return titleSatisfies(issue);
        case "body":
            return bodySatisfies(issue);
        case "milestone":
            return milestoneSatisfies(model, issue);
        case "label":
            return labelsSatisfy(model, issue);
        case "author":
            return authorSatisfies(issue);
        case "assignee":
            return assigneeSatisfies(model, issue);
        case "involves":
        case "user":
            return involvesSatisfies(model, issue);
        case "type":
            return typeSatisfies(issue);
        case "state":
        case "status":
            return stateSatisfies(issue);
        case "has":
            return satisfiesHasConditions(issue);
        case "no":
            return satisfiesNoConditions(issue);
        case "is":
            return satisfiesIsConditions(issue);
        case "created":
            return satisfiesCreationDate(issue);
        case "updated":
	        return satisfiesUpdatedHours(issue);
        case "repo":
            return satisfiesRepo(issue);
        default:
            return false;
        }
    }

	@Override
    public void applyTo(TurboIssue issue, IModel model) throws QualifierApplicationException {
        assert name != null && content != null;

        // The empty qualifier should not be applied to anything
        assert !isEmptyQualifier();

        switch (name) {
        case "title":
        case "desc":
        case "body":
        case "keyword":
            throw new QualifierApplicationException("Unnecessary filter: issue text cannot be changed by dragging");
        case "id":
            throw new QualifierApplicationException("Unnecessary filter: id is immutable");
        case "created":
            throw new QualifierApplicationException("Unnecessary filter: cannot change issue creation date");
        case "has":
        case "no":
        case "is":
            throw new QualifierApplicationException("Ambiguous filter: " + name);
        case "milestone":
            applyMilestone(issue, model);
            break;
        case "label":
            applyLabel(issue, model);
            break;
        case "assignee":
            applyAssignee(issue, model);
            break;
        case "author":
            throw new QualifierApplicationException("Unnecessary filter: cannot change author of issue");
        case "involves":
        case "user":
            throw new QualifierApplicationException("Ambiguous filter: cannot change users involved with issue");
        case "state":
        case "status":
            applyState(issue);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean canBeAppliedToIssue() {
        return true;
    }

    @Override
    public List<String> getQualifierNames() {
        return new ArrayList<>(Arrays.asList(name));
    }

	@Override
	public FilterExpression filter(Predicate<Qualifier> pred) {
		if (pred.test(this)) {
			return new Qualifier(this);
		} else {
			return EMPTY;
		}
	}

	@Override
	public List<Qualifier> find(Predicate<Qualifier> pred) {
		if (pred.test(this)) {
			ArrayList<Qualifier> result = new ArrayList<>();
			result.add(this);
			return result;
		} else {
			return new ArrayList<>();
		}
	}

	/**
     * This method is used to serialise qualifiers. Thus whatever form returned
     * should be syntactically valid.
     */
    @Override
    public String toString() {
        if (this == EMPTY) {
            return "";
        } else if (content.isPresent()) {
            if (name.equals("keyword")) {
                return content.get();
            } else {
            	String quotedContent = content.get();
            	if (quotedContent.contains(" ")) {
            		quotedContent = "\"" + quotedContent + "\"";
            	}
                return name + ":" + quotedContent;
            }
        } else if (date.isPresent()) {
            return name + ":" + date.get().toString();
        } else if (dateRange.isPresent()) {
            return name + ":" + dateRange.get().toString();
        } else if (numberRange.isPresent()) {
        	return name + ":" + numberRange.get().toString();
        } else if (number.isPresent()) {
        	return name + ":" + number.get().toString();
        } else {
            assert false : "Should not happen";
            return "";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((dateRange == null) ? 0 : dateRange.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Qualifier other = (Qualifier) obj;
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (dateRange == null) {
            if (other.dateRange != null)
                return false;
        } else if (!dateRange.equals(other.dateRange))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

	private static boolean shouldNotBeStripped(Qualifier q) {
		return !shouldBeStripped(q);
	}

	private static boolean shouldBeStripped(Qualifier q) {
		switch (q.getName()) {
			case "in":
				return true;
			default:
				return false;
		}
	}

	private static boolean isMetaQualifier(Qualifier q) {
		switch (q.getName()) {
		case "in":
		case "repo":
			return true;
		default:
			return false;
		}
	}

    private boolean idSatisfies(TurboIssue issue) {
	    return number.isPresent() && issue.getId() == number.get();
    }

	private boolean satisfiesUpdatedHours(TurboIssue issue) {
		int hours = Utility.safeLongToInt(issue.getUpdatedAt().until(getCurrentTime(), ChronoUnit.HOURS));

		if (numberRange.isPresent()) {
			return numberRange.get().encloses(hours);
		} else if (number.isPresent()) {
			// Treat it as <
			return new NumberRange(null, number.get(), true).encloses(hours);
		} else {
			return false;
		}
	}

	private boolean satisfiesRepo(TurboIssue issue) {
		if (!content.isPresent()) return false;
		return issue.getRepoId().equals(content.get());
	}

    private boolean satisfiesCreationDate(TurboIssue issue) {
    	LocalDate creationDate = issue.getCreatedAt().toLocalDate();
    	if (date.isPresent()) {
    		return creationDate.isEqual(date.get());
    	} else if (dateRange.isPresent()) {
    		return dateRange.get().encloses(creationDate);
    	} else {
    		return false;
    	}
	}

	private boolean satisfiesHasConditions(TurboIssue issue) {
    	if (!content.isPresent()) return false;
        switch (content.get()) {
        case "label":
        case "labels":
            return issue.getLabels().size() > 0;
        case "milestone":
        case "milestones":
            return issue.getMilestone() != null;
        case "assignee":
        case "assignees":
            return issue.getAssignee() != null;
        default:
            return false;
        }
    }

    private boolean satisfiesNoConditions(TurboIssue issue) {
	    return content.isPresent() && !satisfiesHasConditions(issue);
    }

	private boolean satisfiesIsConditions(TurboIssue issue) {
    	if (!content.isPresent()) return false;
        switch (content.get()) {
        case "open":
        case "closed":
            return stateSatisfies(issue);
        case "pr":
        case "issue":
            return typeSatisfies(issue);
        case "merged":
        	return issue.isPullRequest() && !issue.isOpen();
        case "unmerged":
        	return issue.isPullRequest() && issue.isOpen();
        default:
            return false;
        }
    }

	private boolean stateSatisfies(TurboIssue issue) {
    	if (!content.isPresent()) return false;
    	String content = this.content.get().toLowerCase();
        if (content.contains("open")) {
            return issue.isOpen();
        } else if (content.contains("closed")) {
            return !issue.isOpen();
        } else {
            return false;
        }
    }

    private boolean assigneeSatisfies(IModel model, TurboIssue issue) {
	    if (!content.isPresent()) return false;
	    Optional<TurboUser> assignee = model.getAssigneeOfIssue(issue);

	    if (!assignee.isPresent()) return false;

	    String content = this.content.get().toLowerCase();
	    String login = assignee.get().getLoginName() == null ? "" : assignee.get().getLoginName().toLowerCase();
	    String name = assignee.get().getRealName() == null ? "" : assignee.get().getRealName().toLowerCase();

	    return login.contains(content) || name.contains(content);
    }

    private boolean authorSatisfies(TurboIssue issue) {
    	if (!content.isPresent()) return false;

        String creator = issue.getCreator();

        return creator.toLowerCase().contains(content.get().toLowerCase());
    }

    private boolean involvesSatisfies(IModel model, TurboIssue issue) {
    	return authorSatisfies(issue) || assigneeSatisfies(model, issue);
    }

    private boolean labelsSatisfy(IModel model, TurboIssue issue) {
    	if (!content.isPresent()) return false;

	    // Make use of TurboLabel constructor to parse the string, to avoid duplication
	    TurboLabel tokens = new TurboLabel("", content.get().toLowerCase());

	    String group = "";
		if (tokens.getGroup().isPresent()) {
			group = tokens.getGroup().get().toLowerCase();
		}
	    String labelName = tokens.getName().toLowerCase();

        for (TurboLabel label : model.getLabelsOfIssue(issue)) {
	        if (label.getGroup().isPresent()) {
		        // Check both
		        if (label.getGroup().get().toLowerCase().contains(group)
			        && label.getName().toLowerCase().contains(labelName)) {
			        return true;
		        }
	        } else {
		        // Check only the label name
		        return label.getName().toLowerCase().contains(labelName);
	        }
        }
        return false;
    }

    private boolean milestoneSatisfies(IModel model, TurboIssue issue) {
    	if (!content.isPresent()) return false;
	    Optional<TurboMilestone> milestone = model.getMilestoneOfIssue(issue);

	    if (!milestone.isPresent()) return false;

	    String contents = content.get().toLowerCase();
	    String title = milestone.get().getTitle().toLowerCase();

        return title.contains(contents);
    }

    private boolean keywordSatisfies(TurboIssue issue, MetaQualifierInfo info) {

    	if (info.getIn().isPresent()) {
    		switch (info.getIn().get()) {
    		case "title":
    	        return titleSatisfies(issue);
    		case "body":
    		case "desc":
    	        return bodySatisfies(issue);
    	    default:
    	    	return false;
    		}
    	} else {
	        return titleSatisfies(issue) || bodySatisfies(issue);
    	}
	}

	private boolean bodySatisfies(TurboIssue issue) {
    	if (!content.isPresent()) return false;
        return issue.getDescription().toLowerCase().contains(content.get().toLowerCase());
    }

	private boolean titleSatisfies(TurboIssue issue) {
    	if (!content.isPresent()) return false;
        return issue.getTitle().toLowerCase().contains(content.get().toLowerCase());
    }

    private boolean typeSatisfies(TurboIssue issue) {
    	if (!content.isPresent()) return false;
    	String content = this.content.get().toLowerCase();
	    switch (content) {
		    case "issue":
			    return !issue.isPullRequest();
		    case "pr":
		    case "pullrequest":
			    return issue.isPullRequest();
		    default:
			    return false;
	    }
	}

	private void applyMilestone(TurboIssue issue, IModel model) throws QualifierApplicationException {
    	if (!content.isPresent()) {
    		throw new QualifierApplicationException("Invalid milestone " + (date.isPresent() ? date.get() : dateRange.get()));
    	}

        // Find milestones containing the partial title
        List<TurboMilestone> milestones = model.getMilestones().stream().filter(m -> m.getTitle().toLowerCase().contains(content.get().toLowerCase())).collect(Collectors.toList());
        if (milestones.size() > 1) {
            throw new QualifierApplicationException("Ambiguous filter: can apply any of the following milestones: " + milestones.toString());
        } else {
            issue.setMilestone(milestones.get(0));
        }
    }

    private void applyLabel(TurboIssue issue, IModel model) throws QualifierApplicationException {
    	if (!content.isPresent()) {
    		throw new QualifierApplicationException("Invalid label " + (date.isPresent() ? date.get() : dateRange.get()));
    	}

        // Find labels containing the label name
        List<TurboLabel> labels = model.getLabels().stream()
           .filter(l -> l.getActualName().toLowerCase().contains(content.get().toLowerCase()))
	        .collect(Collectors.toList());

        if (labels.size() > 1) {
            throw new QualifierApplicationException("Ambiguous filter: can apply any of the following labels: " + labels.toString());
        } else {
            issue.addLabel(labels.get(0));
        }
    }

    private void applyAssignee(TurboIssue issue, IModel model) throws QualifierApplicationException {
    	if (!content.isPresent()) {
    		throw new QualifierApplicationException("Invalid assignee " + (date.isPresent() ? date.get() : dateRange.get()));
    	}

        // Find assignees containing the partial title
        List<TurboUser> assignees = model.getUsers().stream()
	        .filter(c -> c.getLoginName().toLowerCase().contains(content.get().toLowerCase()))
	        .collect(Collectors.toList());

        if (assignees.size() > 1) {
            throw new QualifierApplicationException("Ambiguous filter: can apply any of the following assignees: " + assignees.toString());
        } else {
            issue.setAssignee(assignees.get(0));
        }
    }

    private void applyState(TurboIssue issue) throws QualifierApplicationException {
    	if (!content.isPresent()) {
    		throw new QualifierApplicationException("Invalid state " + (date.isPresent() ? date.get() : dateRange.get()));
    	}

        if (content.get().toLowerCase().contains("open")) {
            issue.setOpen(true);
        } else if (content.get().toLowerCase().contains("closed")) {
            issue.setOpen(false);
        }
    }

	public Optional<Integer> getNumber() {
		return number;
	}

	public Optional<NumberRange> getNumberRange() {
		return numberRange;
	}

	public Optional<DateRange> getDateRange() {
		return dateRange;
	}

	public Optional<String> getContent() {
		return content;
	}

	public Optional<LocalDate> getDate() {
		return date;
	}

	public String getName() {
		return name;
	}
}