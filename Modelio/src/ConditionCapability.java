import com.modeliosoft.modelio.javadesigner.annotations.objid;

@objid ("97d8cf4d-c5a3-4eb5-8547-6d2b73ab68c3")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConditionCapability")
@XmlSeeAlso({

    /*protocol*/
    AhCapability.class,
    DccpCapability.class,
    EspCapability.class,
    Icmp6Capability.class,
    IcmpCapability.class,
    Ipv4Capability.class,
    Ipv6Capability.class,
    L2PktypeCapability.class,
    L3PayloadLengthCapability.class,
    MacBaseCapability.class,
    MobilityHeaderCapability.class,
    PhysdevCapability.class,
    PolicyCapability.class,
    SctpCapability.class,
    TcpCapability.class,
    UdpCapability.class,
    
    /*programming*/
    BpfCapability.class,
    StringCapability.class,
    U32Capability.class,
    
    /*stateful*/
    ClusterCapability.class,
    CgroupCapability.class,
    ConnectionbytesCapability.class,
    ConnLabelCapability.class,
    ConnLimitCapability.class,
    ConnmarkBaseConditionCapability.class,
    ConntrackCapability.class,
    DevGroupCapability.class,
    HashLimitCapability.class,
    HelperBaseCapability.class,
    IpvsBaseCapability.class,
    LimitBaseCapability.class,
    MarkBaseCapability.class,
    NfacctCapability.class,
    OsfCapability.class,
    QuotaBaseCapability.class,
    RecentCapability.class,
    SocketCapability.class,
    StateBaseCapability.class,
    StatisticCapability.class,
    TimeCapability.class,
    
    /*prerouting*/
    RpfilterCapability.class,
    CpuBaseCapability.class,
    
    /*postrouting*/
    OwnerCapability.class,
    RateestConditionCapability.class,    
    
    /*other*/
    CommentCapability.class,
    SetConditionCapability.class,
    InputInterfaceCapability.class,
    OutputInterfaceCapability.class,
    UncleanCapability.class,
    
    
})
public class ConditionCapability extends SecurityCapability {
}
