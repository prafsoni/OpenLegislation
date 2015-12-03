package gov.nysenate.openleg.model.entity;

import com.google.common.collect.ComparisonChain;
import gov.nysenate.openleg.model.base.SessionYear;
import gov.nysenate.openleg.processor.base.ParseError;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionMember extends Member implements Serializable
{
    private static final long serialVersionUID = -8348372884270872363L;

    public static Pattern shortNamePattern = Pattern.compile("([A-Z-_']+)( ([A-Z]))?");

    /** Unique session member id generated by the persistence layer. Maps to a lbdcShortName.
     * A member may have multiple sessionMemberIds in a single session for different representations of their shortname */
    protected int sessionMemberId;

    /** Current mapping to LBDC's representation of the member id.
     *  This shortName is only unique to the scope of a (2 year) session */
    protected String lbdcShortName;

    /** True if the shortname on this member is an alternate shortname */
    protected boolean alternate;

    /** The session year the member is active in. */
    protected SessionYear sessionYear;

    /** Indicates if the member is currently an incumbent. */
    protected boolean incumbent;

    /** The district number the member is serving in during the given session year. */
    protected Integer districtCode;

    /** --- Constructors --- */

    public SessionMember() {}

    public SessionMember(int memberId, SessionYear sessionYear) {
        super(memberId);
        this.sessionYear = sessionYear;
    }

    public SessionMember(SessionMember other) {
        super(other);
        this.sessionMemberId = other.sessionMemberId;
        this.lbdcShortName = other.lbdcShortName;
        this.sessionYear = other.sessionYear;
        this.incumbent = other.incumbent;
        this.districtCode = other.districtCode;
        this.alternate = other.alternate;
    }

    /**
     * Constructs an incomplete member based on a limited amount of information
     *
     * @param lbdcShortName String - The short name of the member as represented in the source data.
     * @param sessionYear SessionYear - The session year in which this member was active.
     * @param chamber Chamber
     * @throws ParseError if the given shortname cannot be parsed
     * @return Member
     */
    public static SessionMember newMakeshiftMember(String lbdcShortName, SessionYear sessionYear, Chamber chamber) throws ParseError {
        if (lbdcShortName == null) {
            throw new ParseError("Attempted to create makeshift member, but lbdcShortName was null!");
        }
        // Assembly members are not already uppercase
        lbdcShortName = lbdcShortName.toUpperCase().trim();
        SessionMember member = new SessionMember();
        member.setLbdcShortName(lbdcShortName);
        member.setSessionYear(sessionYear);
        member.setChamber(chamber);
        member.setIncumbent(sessionYear.equals(SessionYear.current()));

        Matcher shortNameMatcher = shortNamePattern.matcher(lbdcShortName);
        if (shortNameMatcher.matches()) {
            member.setLastName(shortNameMatcher.group(1));
            if (shortNameMatcher.groupCount() == 3) {
                member.setFirstName(shortNameMatcher.group(3));
                member.setFullName((member.getFirstName() != null ? member.getFirstName() +  " " : "") + member.getLastName());
            }
            else {
                member.setFullName(member.getLastName());
            }
        }
        else {
            throw new ParseError("Can not create makeshift member: LBDC shortname '" + lbdcShortName + "' does not match specification");
        }
        return member;
    }

    /** --- Overrides --- */

    /**
     * Ignores LBDC Shortname since there can be multiple variations.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        final SessionMember other = (SessionMember) obj;
        return Objects.equals(this.sessionYear, other.sessionYear) &&
               Objects.equals(this.incumbent, other.incumbent) &&
               Objects.equals(this.districtCode, other.districtCode);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(memberId, sessionYear, chamber, incumbent, districtCode);
    }

    @Override
    public String toString() {
        return lbdcShortName + " (year: " + sessionYear + ", id: " + memberId + ")";
    }

    @Override
    public int compareTo(Person o) {
        int personComparison = super.compareTo(o);
        if (personComparison == 0 && (o instanceof SessionMember)) {
            SessionMember that = (SessionMember) o;
            return ComparisonChain.start()
                    .compare(this.sessionYear, that.sessionYear)
                    .compareFalseFirst(this.alternate, that.alternate)
                    .compare(this.lbdcShortName, that.lbdcShortName)
                    .result();
        }
        return personComparison;
    }

    /** --- Basic Getters/Setters --- */

    public int getSessionMemberId() {
        return sessionMemberId;
    }

    public void setSessionMemberId(int sessionMemberId) {
        this.sessionMemberId = sessionMemberId;
    }

    public boolean isIncumbent() {
        return incumbent;
    }

    public void setIncumbent(boolean incumbent) {
        this.incumbent = incumbent;
    }

    public String getLbdcShortName() {
        return lbdcShortName;
    }

    public void setLbdcShortName(String lbdcShortName) {
        this.lbdcShortName = lbdcShortName;
    }

    public SessionYear getSessionYear() {
        return sessionYear;
    }

    public void setSessionYear(SessionYear sessionYear) {
        this.sessionYear = sessionYear;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }

    public boolean isAlternate() {
        return alternate;
    }

    public void setAlternate(boolean alternate) {
        this.alternate = alternate;
    }
}